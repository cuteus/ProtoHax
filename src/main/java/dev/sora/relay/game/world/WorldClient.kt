package dev.sora.relay.game.world

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityItem
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.*
import org.cloudburstmc.protocol.bedrock.packet.*
import java.util.*

class WorldClient(session: GameSession, eventManager: EventManager) : WorldwideBlockStorage(session, eventManager) {

    val entityMap = mutableMapOf<Long, Entity>()
    val playerList = mutableMapOf<UUID, PlayerListPacket.Entry>()

	private val handleDisconnect = handle<EventDisconnect> {
		entityMap.clear()
		playerList.clear()
	}

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if (packet is StartGamePacket) {
			entityMap.clear()
			playerList.clear()
			dimension = packet.dimensionId
		} else if (packet is AddEntityPacket) {
			entityMap[packet.runtimeEntityId] = EntityUnknown(packet.runtimeEntityId, packet.uniqueEntityId, packet.identifier).apply {
				move(packet.position)
				rotate(packet.rotation)
				handleSetData(packet.metadata)
				handleSetAttribute(packet.attributes)
			}.also {
				session.eventManager.emit(EventEntitySpawn(session, it))
			}
		} else if (packet is AddItemEntityPacket) {
			entityMap[packet.runtimeEntityId] = EntityItem(packet.runtimeEntityId, packet.uniqueEntityId).apply {
				move(packet.position)
				handleSetData(packet.metadata)
			}.also {
				session.eventManager.emit(EventEntitySpawn(session, it))
			}
		} else if (packet is AddPlayerPacket) {
			entityMap[packet.runtimeEntityId] = EntityPlayer(packet.runtimeEntityId, packet.uniqueEntityId, packet.uuid, packet.username).apply {
				move(packet.position.add(0f, EntityPlayer.EYE_HEIGHT, 0f))
				rotate(packet.rotation)
				handleSetData(packet.metadata)
			}.also {
				session.eventManager.emit(EventEntitySpawn(session, it))
			}
		} else if (packet is RemoveEntityPacket) {
			entityMap.keys.removeIf { (it == packet.uniqueEntityId).also { flag ->
				if (flag) {
					session.eventManager.emit(EventEntityDespawn(session, entityMap[it]!!))
				}
			} }
		} else if (packet is TakeItemEntityPacket) {
			entityMap.remove(packet.itemRuntimeEntityId)
		} else if (packet is PlayerListPacket) {
			val add = packet.action == PlayerListPacket.Action.ADD
			packet.entries.forEach {
				if (add) {
					playerList[it.uuid] = it
				} else {
					playerList.remove(it.uuid)
				}
			}
		} else if (packet is ChangeDimensionPacket) {
			dimension = packet.dimension
		} else {
			entityMap.values.forEach { entity ->
				entity.onPacket(packet)
			}
		}
	}
}
