/*
 * Copyright (C) 2022 Monun
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

package io.github.monun.tap.v1_21_10.item

import io.github.monun.tap.item.ItemSupport
import net.minecraft.nbt.NbtOps
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.inventory.PlayerInventory as BukkitPlayerInventory

class NMSItemSupport : ItemSupport {
    override fun saveToJsonString(item: BukkitItemStack): String {
        val nmsItem = CraftItemStack.asNMSCopy(item)

        val nbt = ItemStack.CODEC
            .encodeStart(NbtOps.INSTANCE, nmsItem)
            .getOrThrow()

        return nbt.toString()
    }

    override fun damageArmor(playerInventory: BukkitPlayerInventory, attackDamage: Double) {
        if (attackDamage <= 0.0) return

        val player = (playerInventory.holder as CraftPlayer).handle
        val level = player.level()

        var damage = (attackDamage / 4.0).toFloat()
        if (damage < 1.0f) damage = 1.0f
        val intDamage = damage.toInt()

        for (slot in EquipmentSlot.entries) {
            if (!slot.isArmor) continue

            val nmsSlot = CraftEquipmentSlot.getNMS(slot)
            val item = player.getItemBySlot(nmsSlot)

            if (item.isEmpty) continue

            item.hurtAndBreak(
                intDamage,
                level,
                player
            ) {}
        }
    }

    override fun damageSlot(playerInventory: BukkitPlayerInventory, slot: EquipmentSlot, damage: Int) {
        if (damage <= 0) return

        val nmsInventory = (playerInventory as CraftInventoryPlayer).inventory
        val nmsItem = nmsInventory.getItem(slot)
        val player = (playerInventory.holder as CraftPlayer).handle
        val level = player.level()

        if (nmsItem.isEmpty) return

        nmsItem.hurtAndBreak(
            damage,
            level,
            player
        ) {}
    }
}

internal fun Inventory.getItem(slot: EquipmentSlot): ItemStack {
    return player.getItemBySlot(CraftEquipmentSlot.getNMS(slot))
}