package classic_utilities.forge.enchant;

import classic_utilities.enchant.EnchantPreview;
import classic_utilities.network.EnchantProtocol;
import classic_utilities.util.RandomCopy;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import net.minecraft.src.EnchantmentData;
import net.minecraft.src.EnchantmentHelper;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ContainerEnchantment;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.World;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnchantSync implements ITickHandler {
    public static final EnchantSync INSTANCE = new EnchantSync();

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private final EnumSet<TickType> ticks = EnumSet.of(TickType.WORLD);

    private final Map<String, Long> lastSent = new HashMap<String, Long>();

    private static Field randomField;
    private static boolean randomFieldSearched;

    private EnchantSync() {
    }

    @Override
    public void tickStart(EnumSet<TickType> types, Object... tickData) {
    }

    @Override
    public void tickEnd(EnumSet<TickType> types, Object... tickData) {
        if(tickData.length == 0 || !(tickData[0] instanceof World)) return;

        World world = (World) tickData[0];
        if(world.isRemote) return;

        for(Object o : world.playerEntities) {
            if(!(o instanceof EntityPlayerMP)) continue;

            EntityPlayerMP player = (EntityPlayerMP) o;
            if(player.craftingInventory instanceof ContainerEnchantment) {
                update(player, (ContainerEnchantment) player.craftingInventory);
            } else {
                lastSent.remove(player.username);
            }
        }
    }

    @Override
    public EnumSet<TickType> ticks() {
        return ticks;
    }

    @Override
    public String getLabel() {
        return "Classic Utilities enchant preview";
    }

    private void update(EntityPlayerMP player, ContainerEnchantment table) {
        Random random = readRandom(table);
        if(random == null) return;

        Long fingerprint = Long.valueOf(fingerprint(table, random));
        Long previous = lastSent.put(player.username, fingerprint);
        if(fingerprint.equals(previous)) return;

        EnchantPreview preview = computePreview(table);
        if(preview == null) return;

        try {
            byte[] payload = EnchantProtocol.encode(preview);

            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = EnchantProtocol.CHANNEL;
            packet.data = payload;
            packet.length = payload.length;

            PacketDispatcher.sendPacketToPlayer(packet, (Player) player);
        } catch(IOException e) {
            LOG.log(Level.WARNING, "Classic Utilities: could not encode enchant preview", e);
        }
    }

    private static long fingerprint(ContainerEnchantment table, Random random) {
        ItemStack stack = table.tableInventory.getStackInSlot(0);

        long h = 1125899906842597L;
        h = 31L * h + table.windowId;
        for(int i = 0; i < EnchantPreview.SLOTS; i++) {
            h = 31L * h + table.enchantLevels[i];
        }
        h = 31L * h + (stack == null ? -1L : stack.itemID * 32768L + stack.getItemDamage());
        h = 31L * h + RandomCopy.stateHash(random);
        return h;
    }

    public static EnchantPreview computePreview(ContainerEnchantment table) {
        Random random = readRandom(table);
        if(random == null) return null;

        ItemStack stack = table.tableInventory.getStackInSlot(0);

        int[] costs = new int[EnchantPreview.SLOTS];
        int[][] ids = new int[EnchantPreview.SLOTS][];
        int[][] strengths = new int[EnchantPreview.SLOTS][];

        for(int slot = 0; slot < EnchantPreview.SLOTS; slot++) {
            ids[slot] = new int[0];
            strengths[slot] = new int[0];

            int cost = table.enchantLevels[slot];
            if(stack == null || cost <= 0) continue;
            costs[slot] = cost;

            Random copy = RandomCopy.copy(random);
            if(copy == null) return null;

            List list = EnchantmentHelper.buildEnchantmentList(copy, stack, cost);
            if(list == null || list.isEmpty()) continue;

            int from = 0;
            int to = list.size();
            if(stack.itemID == Item.book.shiftedIndex) {
                from = copy.nextInt(list.size());
                to = from + 1;
            }

            ids[slot] = new int[to - from];
            strengths[slot] = new int[to - from];
            for(int i = from; i < to; i++) {
                EnchantmentData data = (EnchantmentData) list.get(i);
                ids[slot][i - from] = data.enchantmentobj.effectId;
                strengths[slot][i - from] = data.enchantmentLevel;
            }
        }

        return new EnchantPreview(table.windowId, costs, ids, strengths);
    }

    private static Random readRandom(ContainerEnchantment table) {
        if(!randomFieldSearched) {
            randomFieldSearched = true;
            for(Field field : ContainerEnchantment.class.getDeclaredFields()) {
                if(field.getType() == Random.class) {
                    try {
                        field.setAccessible(true);
                        randomField = field;
                    } catch(RuntimeException e) {
                        LOG.log(Level.WARNING, "Classic Utilities: enchant preview unavailable, field not accessible", e);
                    }
                    break;
                }
            }
            if(randomField == null) {
                LOG.warning("Classic Utilities: enchant preview unavailable, could not find the enchanting table's Random");
            }
        }

        if(randomField == null) return null;

        try {
            return (Random) randomField.get(table);
        } catch(IllegalAccessException e) {
            return null;
        }
    }
}
