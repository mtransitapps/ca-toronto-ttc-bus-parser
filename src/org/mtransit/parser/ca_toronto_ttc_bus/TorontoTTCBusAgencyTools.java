package org.mtransit.parser.ca_toronto_ttc_bus;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
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

// http://www1.toronto.ca/wps/portal/contentonly?vgnextoid=96f236899e02b210VgnVCM1000003dd60f89RCRD
// http://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
public class TorontoTTCBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-toronto-ttc-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new TorontoTTCBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating TTC bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating TTC bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final String RTS_1S = "1S";
	private static final long RID_1S = 10001l;

	private static final String RTS_2S = "2S";
	private static final long RID_2S = 10002l;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (RTS_1S.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_1S;
		} else if (RTS_2S.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return RID_2S;
		}
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "B80000"; // RED (AGENCY WEB SITE CSS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_00529F = "00529F"; // BLUE (NIGHT BUSES)

	private static final String COLOR_FFC41E = "FFC41E"; // 1 - Yellow (web site CSS)
	private static final String COLOR_2B720A = "2B720A"; // 2 - Green (web site CSS)

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (RTS_1S.equalsIgnoreCase(gRoute.getRouteShortName())) {
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

	private static final String WESTBOUND = "westbound";
	private static final String WEST = "west";
	private static final String EASTBOUND = "eastbound";
	private static final String EAST = "east";
	private static final String SOUTHBOUND = "southbound";
	private static final String SOUTH = "south";
	private static final String NORTHBOUND = "northbound";
	private static final String NORTH = "north";

	private static final String TOWARDS = " towards ";
	private static final String VIA = " via ";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String gTripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
		if (gTripHeadsignLC.startsWith(NORTH) || gTripHeadsignLC.endsWith(NORTH) || gTripHeadsignLC.endsWith(NORTHBOUND)) {
			mTrip.setHeadsignDirection(MDirectionType.NORTH);
			return;
		} else if (gTripHeadsignLC.startsWith(SOUTH) || gTripHeadsignLC.endsWith(SOUTH) || gTripHeadsignLC.endsWith(SOUTHBOUND)) {
			mTrip.setHeadsignDirection(MDirectionType.SOUTH);
			return;
		} else if (gTripHeadsignLC.startsWith(EAST) || gTripHeadsignLC.endsWith(EAST) || gTripHeadsignLC.endsWith(EASTBOUND)) {
			mTrip.setHeadsignDirection(MDirectionType.EAST);
			return;
		} else if (gTripHeadsignLC.startsWith(WEST) || gTripHeadsignLC.endsWith(WEST) || gTripHeadsignLC.endsWith(WESTBOUND)) {
			mTrip.setHeadsignDirection(MDirectionType.WEST);
			return;
		}
		if (mRoute.getId() == 36l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		}
		int indexOf = gTripHeadsignLC.indexOf(TOWARDS);
		if (indexOf >= 0) {
			gTripHeadsignLC = gTripHeadsignLC.substring(indexOf + TOWARDS.length());
		}
		indexOf = gTripHeadsignLC.indexOf(VIA);
		if (indexOf >= 0) {
			gTripHeadsignLC = gTripHeadsignLC.substring(0, indexOf);
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTripHeadsignLC), gTrip.getDirectionId());
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern SIDE = Pattern.compile("((^|\\W){1}(side)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String SIDE_REPLACEMENT = "$2$4";

	private static final Pattern EAST_ = Pattern.compile("((^|\\W){1}(east)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EAST_REPLACEMENT = "$2E$4";

	private static final Pattern WEST_ = Pattern.compile("((^|\\W){1}(west)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String WEST_REPLACEMENT = "$2W$4";

	private static final Pattern NORTH_ = Pattern.compile("((^|\\W){1}(north)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String NORTH_REPLACEMENT = "$2N$4";

	private static final Pattern SOUTH_ = Pattern.compile("((^|\\W){1}(south)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String SOUTH_REPLACEMENT = "$2S$4";

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

	private static final Pattern GO = Pattern.compile("((^|\\W){1}(GO)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String GO_REPLACEMENT = "$2GO$4";

	@Override
	public String cleanStopName(String gStopName) {
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

	@Override
	public String getStopCode(GStop gStop) {
		return super.getStopCode(gStop); // stop code used as stop tag by real-time API
	}

	@Override
	public int getStopId(GStop gStop) {
		return super.getStopId(gStop); // stop ID used as stop code by real-time API
	}
}
