package dev.sora.relay.game.entity

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import java.util.*

open class EntityPlayer(runtimeEntityId: Long, uniqueEntityId: Long,
						open val uuid: UUID, open val username: String) : Entity(runtimeEntityId, uniqueEntityId) {

    override fun onPacket(packet: BedrockPacket) {
        super.onPacket(packet)
        if (packet is MovePlayerPacket && packet.runtimeEntityId == runtimeEntityId) {
            move(packet.position.x, packet.position.y, packet.position.z)
            rotate(packet.rotation)
            tickExists++
        }
    }


	override fun toString(): String {
		return "EntityPlayer(entityId=$runtimeEntityId, uniqueId=$uniqueEntityId, username=$username, uuid=$uuid, posX=$posX, posY=$posY, posZ=$posZ)"
	}

	companion object {
		val EYE_HEIGHT = 1.62f
	}
}
