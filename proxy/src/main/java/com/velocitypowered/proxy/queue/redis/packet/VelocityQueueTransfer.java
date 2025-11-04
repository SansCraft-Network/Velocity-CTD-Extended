package com.velocitypowered.proxy.queue.redis.packet;

import com.velocitypowered.proxy.redis.packet.annotation.OneWayPacket;
import com.velocitypowered.proxy.redis.packet.typed.UUIDPacket;

import java.util.UUID;

/**
 * @author Elmar Blume - 04/11/2025
 */
@OneWayPacket
public final class VelocityQueueTransfer extends UUIDPacket {
    
    private final String queueName;
    
    public VelocityQueueTransfer(final UUID uniqueId, final String queueName) {
        super(uniqueId);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }
}
