package lakmoore.sel.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import coloredlightscore.src.api.CLBlock;

public class ItemConfigHelper
{
    private final String SWILDCARD = "*";
    private final int WILDCARD = -1;
    
    private Map<ItemData, Integer> dataMap;
    
    public ItemConfigHelper(String configLine, int defaultValue)
    {
        dataMap = new HashMap<ItemData, Integer>();
        for (String s : configLine.split(","))
        {
            try
            {
                String[] duo = s.split("=");
                dataMap.put(fromString(duo[0]), duo.length > 1 ? Integer.parseInt(duo[1]) : defaultValue);
            }
            catch (Exception e)
            {
                System.err.println("Error, String ["+s+"] is not a valid Entry, skipping.");
                e.printStackTrace();
            }
        }
    }
    
    public String toString()
    {
        String result = "";
        
        Iterator<Entry<ItemData, Integer>> items = dataMap.entrySet().iterator();
        Entry<ItemData, Integer> entry;
        while (items.hasNext())
        {
            entry = items.next();
            if (entry.getValue() > 0)
            {
                if (result.length() > 0) result += ",";
                result += entry.getKey().toString() + "=" + entry.getValue();
            }
        }        

        return result;
    }
   
    public void addItem(ItemStack stack, int lightLevel)
    {
        if (stack != null)
        {
            ResourceLocation name = stack.getItem().getRegistryName();  
            if (name != null)
            {
        		ItemData item;
        		Iterator<ItemData> items = dataMap.keySet().iterator();
        		while (items.hasNext())
                {
            			item = items.next();
            			if (item.matches(name, stack.getMetadata()))
            			{
                        //if we already have an entry, delete it
                        items.remove();
                    }
                }

                //add the new entry
                dataMap.put(new ItemData(name, stack.getMetadata(), WILDCARD), lightLevel);
            }
        }                
    }
    
    public int getLightFromItemStack(ItemStack stack)
    {
        if (stack != null)
        {
            int r = retrieveValue(stack.getItem().getRegistryName(), stack.getMetadata());
            return r < 0 ? 0 : r;
        }
        return 0;
    }
    
    public int retrieveValue(ResourceLocation name, int meta)
    {
        if (name != null)
        {
            for (ItemData item : dataMap.keySet())
            {
                if (item.matches(name, meta))
                {
                    int val = dataMap.get(item);
                    if (val == WILDCARD)
                    {
                        Block b = GameRegistry.findRegistry(Block.class).getValue(name);
                        if (b instanceof CLBlock)
                        {
                            return ((CLBlock)b).getColorLightValue(meta);
                        }
                        return b != null ? b.getDefaultState().getLightValue() : 0;
                    }
                    return val;
                }
            }
        }
        return -1;
    }
    
    /**
     * Possible setups:
     * X := simple ID X, wildcards metadata
     * X-Y := simple ID X and metadata Y
     * X-Y-Z := simple ID X, metadata range Y to Z
     * @param s trimmed String input, matching one of the setups
     * @return ItemData instance
     */
    private ItemData fromString(String s)
    {
        String[] strings = s.split("-");
        int len = strings.length;
        int sm = len > 1 ? catchWildcard(strings[len > 3 ? 2 : 1]) : WILDCARD;
        int em = len > 2 ? catchWildcard(strings[len > 3 ? 3 : 2]) : sm;
                
        ResourceLocation name = new ResourceLocation(strings[0]);
        
        return new ItemData(name, sm, em);
    }
    
    private int catchWildcard(String s)
    {
        if (s.equals(SWILDCARD))
        {
            return WILDCARD;
        }
        return Integer.parseInt(s);
    }
    
    private class ItemData
    {
        private ResourceLocation nameOf;
        final int startMeta;
        final int endMeta;
        
        public ItemData(ResourceLocation name, int startmetarange, int endmetarange)
        {
            nameOf = name;
            startMeta = startmetarange;
            endMeta = endmetarange;
        }
        
        @Override
        public String toString()
        {
            return nameOf										//minecraft:torch
                    + (startMeta < 1 ? "" : "-" + startMeta)		//-0
                    + (endMeta < 1 ? "" : "-" + endMeta);
        }
        
        public boolean matches(ResourceLocation name, int meta)
        {
            return name.equals(nameOf) && isContained(startMeta, endMeta, meta);
        }
        
        private boolean isContained(int s, int e, int i)
        {
            return (s == WILDCARD || i >= s) && (e == WILDCARD || i <= e);
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o instanceof ItemData)
            {
                ItemData i = (ItemData) o;
                return i.nameOf.equals(nameOf) && i.startMeta == startMeta && i.endMeta == endMeta;
            }
            return false;
        }
        
        @Override
        public int hashCode()
        {
            return nameOf.hashCode() + startMeta + endMeta;
        }
    }
    
}
