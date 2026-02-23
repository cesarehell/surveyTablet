package com.mr.restaurant.survey.data.sync

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RtdbSyncListener(
	private val tenantId: String,
	private val prefs: RtdbSyncPrefs,
	private val scope: CoroutineScope,
	private val onTrigger: suspend () -> Unit
) {

	private var ref: DatabaseReference? = null
	private var listener: ChildEventListener? = null

	fun start() {
		val r = FirebaseDatabase.getInstance()
			.getReference(RtdbEvents.TENANTS)
			.child(tenantId)
			.child(RtdbEvents.EVENTS)

		ref = r

		listener = object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
				handle(snapshot)
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				handle(snapshot)
			}

			override fun onChildRemoved(snapshot: DataSnapshot) = Unit
			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
			override fun onCancelled(error: DatabaseError) {
				Log.e("RtdbSyncListener", "RTDB cancelled: ${error.message}", error.toException())
			}

			private fun handle(snapshot: DataSnapshot) {
				val eventId = snapshot.key ?: return
				val type = snapshot.child(RtdbEvents.TYPE).getValue(String::class.java) ?: "UNKNOWN"
				val entity = snapshot.child(RtdbEvents.ENTITY).getValue(String::class.java)
				val action = snapshot.child(RtdbEvents.ACTION).getValue(String::class.java)

				scope.launch {
					val last = prefs.getLastEventId()
					if (last == eventId) return@launch

					Log.d("RtdbSyncListener", "Event received id=$eventId type=$type entity=$entity action=$action")
					prefs.setLastEventId(eventId)

					val shouldRefresh = when {
						// Legacy/alternative payloads
						type.startsWith("TEMPLATE_") || type.startsWith("RULE_") -> true
						// Current backend payload for config changes
						type == RtdbEvents.CONFIG_CHANGED &&
							(entity == RtdbEvents.ENTITY_TEMPLATE || entity == RtdbEvents.ENTITY_RULE) -> true
						else -> false
					}

					if (shouldRefresh) {
						onTrigger()
					}
				}
			}
		}.also { r.addChildEventListener(it) }

		Log.d("RtdbSyncListener", "Started tenantId=$tenantId path=${r.path}")
	}

	fun stop() {
		val r = ref
		val l = listener
		if (r != null && l != null) r.removeEventListener(l)
		ref = null
		listener = null
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
