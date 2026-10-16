package com.simibubi.create.content.trains.display;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;

import net.createmod.catnip.data.Glob;

import net.minecraft.network.chat.MutableComponent;

public class GlobalTrainDisplayData {

	public static final Map<String, Collection<TrainDeparturePrediction>> statusByDestination = new HashMap<>();
	public static boolean updateTick = false;

	// Caches the last compiled filter pattern and the last prepared result, so
	// repeated display refreshes don't recompile the regex or re-sort the data.
	private static int version;
	private static boolean hasCachedPattern;
	private static String cachedPatternFilter;
	private static Pattern cachedPattern;

	private static boolean hasCachedResult;
	private static String cachedResultFilter;
	private static int cachedResultMaxLines;
	private static int cachedResultVersion;
	private static List<TrainDeparturePrediction> cachedResult = List.of();

	public static void refresh() {
		statusByDestination.clear();
		for (Train train : Create.RAILWAYS.trains.values()) {
			if (train.runtime.paused || train.runtime.getSchedule() == null)
				continue;
			if (train.derailed || train.graph == null)
				continue;
			for (TrainDeparturePrediction prediction : train.runtime.submitPredictions())
				statusByDestination.computeIfAbsent(prediction.destination, $ -> new ArrayList<>())
					.add(prediction);
		}
		version++;
	}

	public static void clear() {
		statusByDestination.clear();
		version++;
	}

	private static Pattern patternFor(String filter) {
		if (hasCachedPattern && Objects.equals(filter, cachedPatternFilter))
			return cachedPattern;
		Pattern pattern = Pattern.compile(Glob.toRegexPattern(filter, ""));
		cachedPatternFilter = filter;
		cachedPattern = pattern;
		hasCachedPattern = true;
		return pattern;
	}

	public static List<TrainDeparturePrediction> prepare(String filter, int maxLines) {
		if (hasCachedResult && cachedResultVersion == version && cachedResultMaxLines == maxLines
			&& Objects.equals(filter, cachedResultFilter))
			return cachedResult;

		Pattern pattern = patternFor(filter);
		List<TrainDeparturePrediction> result = statusByDestination.entrySet()
			.stream()
			.filter(e -> pattern.matcher(e.getKey())
				.matches())
			.flatMap(e -> e.getValue()
				.stream())
			.sorted()
			.limit(maxLines)
			.toList();
		cachedResult = result;
		cachedResultFilter = filter;
		cachedResultMaxLines = maxLines;
		cachedResultVersion = version;
		hasCachedResult = true;
		return result;
	}

	public static class TrainDeparturePrediction implements Comparable<TrainDeparturePrediction> {
		public Train train;
		public int ticks;
		public MutableComponent scheduleTitle;
		public String destination;

		public TrainDeparturePrediction(Train train, int ticks, MutableComponent scheduleTitle, String destination) {
			this.scheduleTitle = scheduleTitle;
			this.destination = destination;
			this.train = train;
			this.ticks = ticks;
		}

		private int getCompareTicks() {
			if (ticks == -1)
				return Integer.MAX_VALUE;
			if (ticks < 200)
				return 0;
			return ticks;
		}

		@Override
		public int compareTo(TrainDeparturePrediction o) {
			int compare = Integer.compare(getCompareTicks(), o.getCompareTicks());
			if (compare == 0)
				return train.name.getString()
					.compareTo(o.train.name.getString());
			return compare;
		}

	}

}
