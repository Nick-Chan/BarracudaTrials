package com.BarracudaTrials.util;

import com.BarracudaTrials.model.Difficulty;
import com.BarracudaTrials.model.RouteVariant;
import com.BarracudaTrials.model.Trial;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.coords.WorldPoint;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class JubblyPillarData
{
    private JubblyPillarData() {}

    private static final Map<String, List<PillarDef>> CACHE = new HashMap<>();

    public static List<PillarDef> getPillars(Trial trial,
                                             Difficulty difficulty,
                                             RouteVariant variant)
    {
        String path = RouteResources.buildPillarsPath(trial, difficulty, variant);
        return CACHE.computeIfAbsent(path, JubblyPillarData::loadPillars);
    }

    public static final class PillarDef
    {
        public final int regionId;
        public final int regionX;
        public final int regionY;
        public final int plane;
        public final int order;
        public final int pillar;
        public final int varbitId;

        public PillarDef(int regionId, int regionX, int regionY, int plane,
                         int order, int pillar, int varbitId)
        {
            this.regionId = regionId;
            this.regionX = regionX;
            this.regionY = regionY;
            this.plane = plane;
            this.order = order;
            this.pillar = pillar;
            this.varbitId = varbitId;
        }

        public WorldPoint toWorldPoint()
        {
            return WorldPoint.fromRegion(regionId, regionX, regionY, plane);
        }
    }

    // JSON
    private static final class PillarPointDto
    {
        int regionId;
        int regionX;
        int regionY;
        int z;
        int order;
        int pillar;
        int varbitId;
    }

    private static List<PillarDef> loadPillars(String resourcePath)
    {
        InputStream in = JubblyPillarData.class.getResourceAsStream(resourcePath);
        if (in == null)
        {
            return Collections.emptyList();
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<PillarPointDto>>(){}.getType();
            List<PillarPointDto> points = gson.fromJson(reader, listType);

            if (points == null)
            {
                return Collections.emptyList();
            }

            return points.stream()
                    .map(p -> new PillarDef(
                            p.regionId,
                            p.regionX,
                            p.regionY,
                            p.z,
                            p.order,
                            p.pillar,
                            p.varbitId
                    ))
                    .sorted(Comparator.comparingInt(def -> def.order))
                    .collect(Collectors.toList());
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }
}
