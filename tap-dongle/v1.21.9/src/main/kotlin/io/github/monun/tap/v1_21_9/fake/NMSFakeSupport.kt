/*
 * Copyright (C) 2023 Monun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.monun.tap.v1_21_9.fake

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import io.github.monun.tap.fake.FakeSkinParts
import io.github.monun.tap.fake.FakeSupport
import io.github.monun.tap.v1_21_9.protocol.NMSPacketContainer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.*
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * @author Nemo
 */
class NMSFakeSupport : FakeSupport {

    override fun getNetworkId(entity: Entity): Int {
        entity as CraftEntity

        return BuiltInRegistries.ENTITY_TYPE.getId(entity.handle.type)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Entity> createEntity(entityClass: Class<out Entity>, world: World): T {
        return NMSEntityTypes.findType(entityClass).run {
            val nmsWorld = (world as CraftWorld).handle
            this.create(nmsWorld, EntitySpawnReason.COMMAND)?.bukkitEntity as T
        }
    }

    override fun isInvisible(entity: Entity): Boolean {
        entity as CraftEntity
        val nmsEntity = entity.handle

        return nmsEntity.isInvisible
    }

    override fun setInvisible(entity: Entity, invisible: Boolean) {
        entity as CraftEntity
        val nmsEntity = entity.handle

        nmsEntity.isInvisible = invisible
    }

    private val nmsPoses = net.minecraft.world.entity.Pose.values()
    override fun setPose(entity: Entity, pose: Pose) {
        (entity as CraftEntity).handle.pose = nmsPoses[pose.ordinal]
    }

    override fun setLocation(
        entity: Entity,
        loc: Location
    ) {
        entity as CraftEntity
        val nmsEntity = entity.handle

        loc.run {
            nmsEntity.setLevel((world as CraftWorld).handle)
            nmsEntity.setPos(x, y, z)
            nmsEntity.xRot = pitch
            nmsEntity.yRot = yaw
            nmsEntity.setYBodyRot(yaw)
        }
    }

    override fun getMountedYOffset(entity: Entity): Double {
        entity as CraftEntity

        return entity.handle.getDimensions(entity.handle.pose).height * 0.75
    }

    override fun getYOffset(entity: Entity): Double {
        return 0.0
    }

    /* Modified */
    override fun createSpawnPacket(entity: Entity): Array<NMSPacketContainer> {
        val packets = arrayListOf<NMSPacketContainer>()
        entity as CraftEntity
        if (entity is Player) {
            packets.add(
                NMSPacketContainer(
                    ClientboundPlayerInfoUpdatePacket(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        entity.handle as ServerPlayer
                    )
                )
            )
        }

        val nmsEntity = entity.handle
        val packet = ClientboundAddEntityPacket(nmsEntity.id, nmsEntity.uuid, nmsEntity.x, nmsEntity.y, nmsEntity.z, nmsEntity.xRot, nmsEntity.yRot, nmsEntity.type, 0, nmsEntity.deltaMovement,
            nmsEntity.yHeadRot.toDouble()
        )
        packets.add(NMSPacketContainer(packet))

        return packets.toTypedArray()
    }

    override fun createFallingBlock(blockData: BlockData): FallingBlock {
        val entity =
            FallingBlockEntity(
                (Bukkit.getWorlds().first() as CraftWorld).handle,
                0.0,
                0.0,
                0.0,
                (blockData as CraftBlockData).state
            )

        return entity.bukkitEntity as FallingBlock
    }

    override fun createItemEntity(item: ItemStack): Item {
        val entity =
            ItemEntity(
                (Bukkit.getWorlds().first() as CraftWorld).handle,
                0.0,
                0.0,
                0.0,
                CraftItemStack.asNMSCopy(item)
            )
        entity.setNeverPickUp()

        return entity.bukkitEntity as Item
    }

    override fun createPlayerEntity(
        name: String,
        profileProperties: Set<ProfileProperty>,
        skinParts: FakeSkinParts,
        uniqueId: UUID
    ): Player {
        val playerProfile = Bukkit.createProfile(uniqueId, name)
        for (property in profileProperties) {
            playerProfile.setProperty(ProfileProperty(property.name, property.value, property.signature))
        }
        val gameProfile = (playerProfile as CraftPlayerProfile).buildGameProfile()

        val player = ServerPlayer(
            (Bukkit.getServer() as CraftServer).handle.server,
            (Bukkit.getWorlds().first() as CraftWorld).handle,
            gameProfile,
            ClientInformation.createDefault()
        )

        player.entityData.set(
            ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION,
            skinParts.raw.toByte()
        )

        player.connection = ServerGamePacketListenerImpl(
            (Bukkit.getServer() as CraftServer).handle.server,
            Connection(PacketFlow.SERVERBOUND),
            player,
            CommonListenerCookie.createInitial(gameProfile, false)
        )

        return player.bukkitEntity
    }

    override fun setSkinParts(player: Player, raw: Int) {
        (player as CraftPlayer).handle.apply {
            entityData.set(ServerPlayer.DATA_PLAYER_MODE_CUSTOMISATION, raw.toByte())
        }
    }
}