package com.mr.restaurant.survey.data.sync

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RtdbSyncListener(
	private val tenantId: String,
	private val prefs: RtdbSyncPrefs,
	private val scope: CoroutineScope,
	private val onTrigger: suspend () -> Unit
) {

	private var ref: DatabaseReference? = null
	private var listener: ChildEventListener? = null
	private var debounceJob: Job? = null
	private var primed = false
	private var lastEventIdInMemory: String? = null
	private var initialEventIds: MutableSet<String> = mutableSetOf()

	fun start() {
		val r = FirebaseDatabase.getInstance()
			.getReference(RtdbEvents.TENANTS)
			.child(tenantId)
			.child(RtdbEvents.EVENTS)

		ref = r
		primed = false
		initialEventIds.clear()

		// Primer paso: capturar backlog actual para no procesarlo como "cambio nuevo".
		r.addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				initialEventIds = snapshot.children
					.mapNotNull { it.key }
					.toMutableSet()
				attachChildListener(r)
				primed = true
				Log.d(
					"RtdbSyncListener",
					"Started tenantId=$tenantId (backlog=${initialEventIds.size})"
				)
			}

			override fun onCancelled(error: DatabaseError) {
				Log.e("RtdbSyncListener", "RTDB prime cancelled: ${error.message}", error.toException())
				attachChildListener(r)
				primed = true
				Log.d("RtdbSyncListener", "Started tenantId=$tenantId (without prime)")
			}
		})
	}

	private fun attachChildListener(r: DatabaseReference) {
		listener = object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) = handle(snapshot)
			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = handle(snapshot)
			override fun onChildRemoved(snapshot: DataSnapshot) = Unit
			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
			override fun onCancelled(error: DatabaseError) {
				Log.e("RtdbSyncListener", "RTDB cancelled: ${error.message}", error.toException())
			}

			private fun handle(snapshot: DataSnapshot) {
				val eventId = snapshot.key ?: return
				if (!primed) return

				// Ignorar backlog inicial al arrancar para evitar tormenta de refresh.
				if (initialEventIds.remove(eventId)) return

				val type = snapshot.child(RtdbEvents.TYPE).getValue(String::class.java) ?: "UNKNOWN"
				val entity = snapshot.child(RtdbEvents.ENTITY).getValue(String::class.java)
				val action = snapshot.child(RtdbEvents.ACTION).getValue(String::class.java)

				scope.launch {
					if (lastEventIdInMemory == null) {
						lastEventIdInMemory = prefs.getLastEventId()
					}
					if (lastEventIdInMemory == eventId) return@launch

					Log.d("RtdbSyncListener", "Event received id=$eventId type=$type entity=$entity action=$action")
					lastEventIdInMemory = eventId
					prefs.setLastEventId(eventId)

					val shouldRefresh = when {
						// Legacy/alternative payloads
						type.startsWith("TEMPLATE_") || type.startsWith("RULE_") -> true
						// Current backend payload for config changes
						type == RtdbEvents.CONFIG_CHANGED -> true

						else -> false
					}

					if (shouldRefresh) {
						// Coalesce de ráfagas: varios eventos => un solo refresh.
						debounceJob?.cancel()
						debounceJob = scope.launch {
							delay(450)
							onTrigger()
						}
					}
				}
			}
		}.also { r.addChildEventListener(it) }
	}

	fun stop() {
		val r = ref
		val l = listener
		if (r != null && l != null) r.removeEventListener(l)
		debounceJob?.cancel()
		debounceJob = null
		ref = null
		listener = null
		primed = false
		initialEventIds.clear()
		Log.d("RtdbSyncListener", "Stopped tenantId=$tenantId")
	}
}

object RtdbEvents {
	const val TENANTS = "tenants"
	const val EVENTS = "events"
	const val TYPE = "type"
	const val ENTITY = "entity"
	const val ACTION = "action"

	const val CONFIG_CHANGED = "CONFIG_CHANGED"
	const val ENTITY_TEMPLATE = "TEMPLATE"
	const val ENTITY_RULE = "RULE"
}
