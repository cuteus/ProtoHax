package dev.sora.relay.game.management

import dev.sora.relay.game.event.*
import dev.sora.relay.utils.logError
import io.netty.buffer.ByteBuf
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheBlobStatusPacket
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheMissResponsePacket
import java.util.function.Predicate

/**
 * manages BLOB(Binary Large OBjects) cache
 */
class BlobCacheManager(override val eventManager: EventManager) : Listenable {

    private val clientAcknowledgements = mutableListOf<Long>()
    private val cacheCallbacks = mutableMapOf<Long, (ByteBuf) -> Unit>()

    fun registerCacheCallback(blobId: Long, callback: (ByteBuf) -> Unit) {
        cacheCallbacks[blobId] = callback
    }

	private val handleDisconnect = handle<EventDisconnect> {
		clientAcknowledgements.clear()
		cacheCallbacks.clear()
	}

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		if (packet is ClientCacheBlobStatusPacket) {
			// sync the cache denylist
			clientAcknowledgements.addAll(packet.acks)
			clientAcknowledgements.removeIf { packet.naks.contains(it) }

			// because of we don't have such cache system, we just request cache which we required
			packet.naks.addAll(packet.acks.filter { cacheCallbacks.containsKey(it) })
			packet.acks.removeIf(Predicate { t -> packet.naks.contains(t) })
		} /*else if (packet is ResourcePacksInfoPacket) {
            // attempt disable cache
            val cacheStatusPacket = ClientCacheStatusPacket().apply {
                isSupported = false
            }
            event.session.sendPacket(cacheStatusPacket)
        } else if (packet is ClientCacheStatusPacket) {
            packet.isSupported = false
        }*/
	}

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if (packet is ClientCacheMissResponsePacket) {
			// call cache callback
			packet.blobs.forEach { (id, data) ->
				cacheCallbacks[id]?.let {
					try {
						it(data)
					} catch (t: Throwable) {
						logError("cache callback", t)
					}
				}
			}
			// prevent satisfied caches be sent to client
			packet.blobs.keys.map { it }.forEach {
				if (clientAcknowledgements.contains(it)) {
					packet.blobs.remove(it)
				}
			}
			if (packet.blobs.isEmpty()) {
				event.cancel()
			}
		}
	}
}
