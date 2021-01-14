package org.mtransit.parser.ca_toronto_ttc_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// https://open.toronto.ca/dataset/ttc-routes-and-schedules/
// OLD: http://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
// http://opendata.toronto.ca/toronto.transit.commission/ttc-routes-and-schedules/OpenData_TTC_Schedules.zip
public class TorontoTTCBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-toronto-ttc-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new TorontoTTCBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating TTC bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating TTC bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if ("NOT IN SERVICE".equals(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final String RTS_1A = "1A";
	private static final long RID_1A = 10_003L;

	private static final String RTS_1S = "1S";
	private static final long RID_1S = 10_001L;

	private static final String RTS_2S = "2S";
	private static final long RID_2S = 10_002L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (RTS_1A.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_1A;
		} else if (RTS_1S.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_1S;
		} else if (RTS_2S.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_2S;
		}
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (routeLongName == null) {
			routeLongName = EMPTY;
		}
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "B80000"; // RED (AGENCY WEB SITE CSS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_00529F = "00529F"; // BLUE (NIGHT BUSES)

	private static final String COLOR_FFC41E = "FFC41E"; // 1 - Yellow (web site CSS)
	private static final String COLOR_2B720A = "2B720A"; // 2 - Green (web site CSS)

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (RTS_1A.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_FFC41E; // same as subway line 1
		} else if (RTS_1S.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_FFC41E; // same as subway line 1
		} else if (RTS_2S.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return COLOR_2B720A; // same as subway line 2
		}
		int rsn = Integer.parseInt(gRoute.getRouteShortName());
		if (rsn >= 300 && rsn <= 399) { // Night Network
			return COLOR_00529F;
		}
		return null; // use agency color instead of provided colors (like web site)
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_DASH_ = Pattern.compile("( - .*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		if ("EAST - 84 SHEPPARD WEST towards SHEPPARD-YONGE STATION".equalsIgnoreCase(directionHeadSign)) {
			return "West"; // wrong direction ID
		}
		directionHeadSign = STARTS_WITH_DASH_.matcher(directionHeadSign).replaceAll(EMPTY); // keep East/West/North/South
		directionHeadSign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, directionHeadSign);
		return CleanUtils.cleanLabel(directionHeadSign);
	}

	@Override
	public int getDirectionType() {
		return MTrip.HEADSIGN_TYPE_DIRECTION;
	}

	private static final String WEST = "west";
	private static final String EAST = "east";
	private static final String SOUTH = "south";
	private static final String NORTH = "north";

	@Nullable
	@Override
	public MDirectionType convertDirection(@Nullable String headSign) {
		if (headSign != null) {
			String gTripHeadsignLC = headSign.toLowerCase(Locale.ENGLISH);
			switch (gTripHeadsignLC) {
			case NORTH:
				return MDirectionType.NORTH;
			case SOUTH:
				return MDirectionType.SOUTH;
			case EAST:
				return MDirectionType.EAST;
			case WEST:
				return MDirectionType.WEST;
			default:
				throw new MTLog.Fatal("Unexpected direction '%s' to convert!", headSign);
			}
		}
		return null;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern KEEP_LETTER_AND_TOWARDS_ = Pattern.compile("(^([a-z]+) - ([\\d]+)([a-z]?)( (.*) towards)? (.*))", Pattern.CASE_INSENSITIVE);
	private static final String KEEP_LETTER_AND_TOWARDS_REPLACEMENT = "$4 $7";

	private static final Pattern ENDS_EXTRA_FARE_REQUIRED = Pattern.compile("(( -)? extra fare required .*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SHORT_TURN_ = CleanUtils.cleanWords("short turn");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = KEEP_LETTER_AND_TOWARDS_.matcher(tripHeadsign).replaceAll(KEEP_LETTER_AND_TOWARDS_REPLACEMENT);
		tripHeadsign = ENDS_EXTRA_FARE_REQUIRED.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = SHORT_TURN_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern SIDE = Pattern.compile("((^|\\W)(" + "side" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String SIDE_REPLACEMENT = "$2" + "$4";

	private static final Pattern EAST_ = Pattern.compile("((^|\\W)(" + "east" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EAST_REPLACEMENT = "$2" + "E" + "$4";

	private static final Pattern WEST_ = Pattern.compile("((^|\\W)(" + "west" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String WEST_REPLACEMENT = "$2" + "W" + "$4";

	private static final Pattern NORTH_ = Pattern.compile("((^|\\W)(" + "north" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String NORTH_REPLACEMENT = "$2" + "N" + "$4";

	private static final Pattern SOUTH_ = Pattern.compile("((^|\\W)(" + "south" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String SOUTH_REPLACEMENT = "$2" + "S" + "$4";

	private static final Pattern HS = Pattern.compile("(H\\.S\\.)", Pattern.CASE_INSENSITIVE);
	private static final String HS_REPLACEMENT = "HS";

	private static final Pattern SS = Pattern.compile("(S\\.S\\.)", Pattern.CASE_INSENSITIVE);
	private static final String SS_REPLACEMENT = "SS";

	private static final Pattern CNR = Pattern.compile("(C\\.N\\.R\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CNR_REPLACEMENT = "CNR";

	private static final Pattern CN = Pattern.compile("(C\\.[\\s]*N\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CN_REPLACEMENT = "CN";

	private static final Pattern CI = Pattern.compile("(C\\.I\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CI_REPLACEMENT = "CI";

	private static final Pattern II = Pattern.compile("(II)", Pattern.CASE_INSENSITIVE);
	private static final String II_REPLACEMENT = "II";

	private static final Pattern GO = Pattern.compile("((^|\\W)(" + "GO" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String GO_REPLACEMENT = "$2" + "GO" + "$4";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = SIDE.matcher(gStopName).replaceAll(SIDE_REPLACEMENT);
		gStopName = EAST_.matcher(gStopName).replaceAll(EAST_REPLACEMENT);
		gStopName = WEST_.matcher(gStopName).replaceAll(WEST_REPLACEMENT);
		gStopName = NORTH_.matcher(gStopName).replaceAll(NORTH_REPLACEMENT);
		gStopName = SOUTH_.matcher(gStopName).replaceAll(SOUTH_REPLACEMENT);
		gStopName = HS.matcher(gStopName).replaceAll(HS_REPLACEMENT);
		gStopName = SS.matcher(gStopName).replaceAll(SS_REPLACEMENT);
		gStopName = CNR.matcher(gStopName).replaceAll(CNR_REPLACEMENT);
		gStopName = CN.matcher(gStopName).replaceAll(CN_REPLACEMENT);
		gStopName = CI.matcher(gStopName).replaceAll(CI_REPLACEMENT);
		gStopName = II.matcher(gStopName).replaceAll(II_REPLACEMENT);
		gStopName = GO.matcher(gStopName).replaceAll(GO_REPLACEMENT);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		return super.getStopCode(gStop); // stop code used as stop tag by real-time API
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return super.getStopId(gStop); // stop ID used as stop code by real-time API
	}
}
