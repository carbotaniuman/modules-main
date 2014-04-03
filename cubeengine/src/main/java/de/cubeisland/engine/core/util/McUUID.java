/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.core.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.cubeisland.engine.core.CubeEngine;

public class McUUID
{
    private final static ObjectMapper mapper = new ObjectMapper();
    private static final String MOJANG_API_URL = "https://api.mojang.com/profiles/page/";
    private static final String AGENT = "minecraft";

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Map<String, UUID> getUUIDForNames(List<String> playerNames)
    {
        Map<String, UUID> map = new HashMap<>();
        for (Profile profile : getUUIDForNames0(playerNames))
        {
            try
            {
                map.put(profile.name, getUUIDFromString(profile.id));
            }
            catch (Exception e)
            {
                CubeEngine.getLog().error("Could not convert UUID of: {} ({})", profile.name, profile.id);
            }
        }
        return map;
    }

    private static UUID getUUIDFromString(String id)
    {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" +id.substring(20, 32));
    }

    private static List<Profile> getUUIDForNames0(List<String> playernames)
    {
        ArrayNode node = mapper.createArrayNode();
        for (String playername : playernames)
        {
            ObjectNode criteria = mapper.createObjectNode();
            criteria.put("name", playername);
            criteria.put("agent", AGENT);
            node.add(criteria);
        }
        int i = 1;
        int amount = playernames.size();
        List<Profile> list = new ArrayList<>();
        try
        {
            while (amount > 0)
            {
                HttpURLConnection con = (HttpURLConnection)new URL(MOJANG_API_URL + i++).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setUseCaches(false);
                con.setDoInput(true);
                con.setDoOutput(true);
                DataOutputStream writer = new DataOutputStream(con.getOutputStream());
                writer.write(node.toString().getBytes());
                writer.close();

                ProfileSearchResult results = mapper.readValue(con.getInputStream(), ProfileSearchResult.class);
                list.addAll(Arrays.asList(results.profiles));
                if (results.size == amount)
                {
                    return list;
                }
                amount -= results.size;
            }
        }
        catch (IOException e)
        {
            CubeEngine.getLog().error(e, "Could not retrieve UUID for given names!");
            return list;
        }
        return list;
    }

    public static UUID getUUIDForName(String player)
    {
        Map<String, UUID> uuidForNames = getUUIDForNames(Arrays.asList(player));
        return uuidForNames.get(player);
    }

    public static class Profile
    {
        public String id;
        public String name;
    }

    public static class ProfileSearchResult
    {
        public Profile[] profiles;
        public int size;
    }
}
