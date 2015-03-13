package choreography.model.timeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import choreography.io.FCWLib;
import choreography.model.fcw.FCW;
import choreography.view.music.MusicPaneController;
import choreography.view.sim.FountainSimController;
import choreography.view.sliders.SlidersController;
import choreography.view.timeline.TimelineController;



/**
 * The Timeline class holds the information at what time which slider has to be
 * triggered.
 * <p>
 * Since the convention of the ctl file just allows to contain changes this
 * Timeline class is <b> stateless </b>.
 *
 * @see StatefulTimeline
 */
public class Timeline {

	public static final int OFF = -5;
	private int time;
	private int numChannels;
	private ConcurrentSkipListMap<Integer, ArrayList<FCW>> timeline;
	private ConcurrentSkipListMap<Integer, SortedMap<Integer, Integer>> channelColorMap;
	private StatefulTimeline statefulTimeline;

	public Timeline() {
		timeline = new ConcurrentSkipListMap<Integer, ArrayList<FCW>>();
		statefulTimeline = new StatefulTimeline();
		channelColorMap = new ConcurrentSkipListMap<Integer, SortedMap<Integer, Integer>>();
	}

	public SortedMap<Integer, SortedMap<Integer, Integer>> getChannelColorMap() {
		return channelColorMap;
	}

	public void setChannelColorMap(
			ConcurrentSkipListMap<Integer, SortedMap<Integer, Integer>> channelColorMap) {
		this.channelColorMap = channelColorMap;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getNumChannels() {
		return numChannels;
	}

	public void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}

	/**
	 * @return the timeline
	 */
	public SortedMap<Integer, ArrayList<FCW>> getTimeline() {
		return timeline;
	}

	/**
	 * @return the waterTimeline
	 */
	public SortedMap<Integer, ArrayList<FCW>> getWaterTimeline() {
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> waterTimeline = new ConcurrentSkipListMap<Integer, ArrayList<FCW>>();
		for (Integer i : timeline.keySet()) {
			ArrayList<FCW> actions = timeline.get(new Integer(i));
			ArrayList<FCW> waterActions = null;
			for (FCW f : actions) {
				if (f.getIsWater()) {
					if (waterActions == null) {
						waterActions = new ArrayList<FCW>();
					}
					waterActions.add(f);
				}
				if (waterActions != null) {
					waterTimeline.put(i, waterActions);
				}
			}

		}

		return waterTimeline;
	}

	/**
	 * @return the waterTimeline
	 */
	public SortedMap<Integer, ArrayList<FCW>> getLightTimeline() {
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> lightTimeline = new ConcurrentSkipListMap<Integer, ArrayList<FCW>>();
		for (Integer i : timeline.keySet()) {
			ArrayList<FCW> actions = timeline.get(new Integer(i));
			ArrayList<FCW> lightActions = null;
			for (FCW f : actions) {
				if (!f.getIsWater()) {
					if (lightActions == null) {
						lightActions = new ArrayList<FCW>();
					}
					lightActions.add(f);
				}
				if (lightActions != null) {
					lightTimeline.put(i, lightActions);
				}
			}

		}
		return lightTimeline;
	}

	/**
	 * @param timeline
	 *            the timeline to set
	 */
	public void setTimeline(
			ConcurrentSkipListMap<Integer, ArrayList<FCW>> timeline) {
        for (Entry<Integer, ArrayList<FCW>> entry : timeline.entrySet())
        {
            System.out.println("Time "+entry.getKey() + "  OP: " + entry.getValue());
        }
		this.timeline = timeline;
		statefulTimeline.loadExistingTimeline(timeline);

		time = (int) (MusicPaneController.SONG_TIME * 10); // tenths of a second
		numChannels = countUChannels(getLightTimeline());
		// lightFCWColorMap = new LinkedHashMap<>(numChannels);
		channelColorMap = new ConcurrentSkipListMap<Integer, SortedMap<Integer, Integer>>();

		startAndEndPoints(channelColorMap);
		fillTheSpaces(channelColorMap);
		// populateLightFcwArray();

		// TimelineController.getInstance().rePaint();
	}

	/**
	 * Inserts FCWs int the timeline for fading commands. These are the only
	 * special commands that require two words
	 * 
	 * @param mod
	 * @param start
	 * @param end
	 * @param color
	 * @param intensity
	 */
	public void setFadeFcw(int mod, int start, int end, int color, int intensity) {
		int address = mod + 600;
		int data = end - start;
		FCW first = new FCW(address, data);

		if (intensity == 100)
			intensity = 0;
		data = (intensity * 10) + color;
		FCW last = new FCW(address - 100, data);
		insertIntoTimeline(timeline, end, first);
		insertIntoTimeline(timeline, end, last);
	}

	/**
	 * Adds start and end commands to light timeline. Fills channelColorMap with
	 * data and repaints timeline.
	 * 
	 * @param f
	 *            the new FCW
	 * @param start
	 *            the point where the light turns on
	 * @param end
	 *            the point where the light should turn off
	 */
	public void setLightFcw(FCW f, int start, int end) {
		insertIntoTimeline(timeline, start, f);
		insertIntoTimeline(timeline, end, new FCW(f.getAddr(), 0));
		SortedMap<Integer, Integer> channel = channelColorMap.get(f.getAddr());
		if (channel == null) {
			channel = new ConcurrentSkipListMap<Integer, Integer>();
			channelColorMap.put(f.getAddr(), channel);
		}
		setLightFcwWithRange(channel, start, end, f.getData());

		TimelineController.getInstance().rePaintLightTimeline();

	}

	/**
	 *
	 * @param channel
	 * @param start
	 * @param end
	 * @param color
	 */
	public void setLightFcwWithRange(SortedMap<Integer, Integer> channel,
			int start, int end, int color) {
		for (int i = start; i < end; i++) {
			channel.put(i, color);
		}
	}

	/**
	 *
	 * @param pointInTime
	 * @param f
	 */
	public void setWaterFcwAtPoint(int pointInTime, FCW f) {
		String[] fActions = FCWLib.getInstance().reverseLookupData(f);
		for (String s : fActions) {
			if (s.equals("RESETALL")) {
				ArrayList<FCW> fcws = timeline.get(new Integer(pointInTime));
				for (FCW action : fcws) {
					if (action.getIsWater()) {
						fcws.remove(action);
					}
				}
				timeline.replace(new Integer(pointInTime), fcws);
			}
		}
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> waterTimeline = (ConcurrentSkipListMap<Integer, ArrayList<FCW>>) getWaterTimeline();
		if (checkForCollision(waterTimeline, pointInTime, f)) {
			ArrayList<FCW> exists = waterTimeline.get(pointInTime);
			ListIterator<FCW> it = exists.listIterator();
			while (it.hasNext()) {
				FCW fExists = it.next();
				if (fExists.getAddr() == f.getAddr()) {
					it.remove();
				}
			}
		}

		statefulTimeline.insertIntoTimelineStateful(pointInTime, f);
		insertIntoTimeline(timeline, pointInTime, f);

		TimelineController.getInstance().rePaintWaterTimeline();

		// waterTimeline.get(pointInTime).add(f);
	}

	/*
	 * public void setLightFcwAtPoint(int point, FCW f) {
	 * 
	 * }
	 */

	/*public void fillTheSpaces(
			SortedMap<Integer, SortedMap<Integer, Integer>> channelMap) {
		for (Integer channel : channelMap.keySet()) {
			int start, end, color;
			SortedMap<Integer, Integer> newMap = new ConcurrentSkipListMap<>();
			for (Integer tenth : channelMap.get(channel).keySet()) {
				if (channelMap.get(channel).get(tenth) != 0) {
					start = tenth;
					color = channelMap.get(channel).get(tenth);
					Iterator<Entry<Integer, Integer>> it = channelMap
							.get(channel).tailMap(start + 1).entrySet()
							.iterator();
					while (it.hasNext()) {
						Entry<Integer, Integer> timeColor = it.next();
						if (timeColor.getValue() == 0
								&& timeColor.getKey() != start) {
							end = timeColor.getKey();
							setLightFcwWithRange(newMap, start, end, color);
							break;
						} else if (timeColor.getValue() != color) {
							end = timeColor.getKey();// - 1;
							setLightFcwWithRange(newMap, start, end, color);
							break;
						}
					}
				}
				channelMap.get(channel).putAll(newMap);
			}
		}
	}*/

public void fillTheSpaces(
        SortedMap<Integer, SortedMap<Integer, Integer>> channelMap) {

        for (Integer channel : channelMap.keySet()) {
            int start, end, color, end2;
			SortedMap<Integer, Integer> newMap = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap17 = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap18 = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap19 = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap20 = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap21 = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap22 = new ConcurrentSkipListMap<>();
			SortedMap<Integer, Integer> newMap23 = new ConcurrentSkipListMap<>();

            if(channel == 51 || channel == 50 || channel == 49) {
                //This part will need to be done for each moduel within the corresponding group(ie group a --> modules 1,3,5,7)
                for (Integer tenth : channelMap.get(channel).keySet()) {
                    if (channelMap.get(channel).get(tenth) != 0) {
                        start = tenth;
                        color = channelMap.get(channel).get(tenth);
                        Iterator<Entry<Integer, Integer>> it;
						if(channel == 49) {
							System.out.println("hit 49 at " + start);
							Iterator<Entry<Integer, Integer>> it2 = channelMap.get(49).tailMap(start + 1).entrySet().iterator();

							it = channelMap.get(17).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap17, start, end, color);
							} else {
								setLightFcwWithRange(newMap17, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(19).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap19, start, end, color);
							} else {
								setLightFcwWithRange(newMap19, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(21).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap21, start, end, color);
							} else {
								setLightFcwWithRange(newMap21, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(23).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap23, start, end, color);
							} else {
								setLightFcwWithRange(newMap23, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

						}
						if(channel == 50) {
							System.out.println("hit 50 at " + tenth);

							Iterator<Entry<Integer, Integer>> it2 = channelMap.get(50).tailMap(start + 1).entrySet().iterator();

							it = channelMap.get(18).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap18, start, end, color);
							} else {
								setLightFcwWithRange(newMap18, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(20).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap20, start, end, color);
							} else {
								setLightFcwWithRange(newMap20, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(22).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap22, start, end, color);
							} else {
								setLightFcwWithRange(newMap22, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);


						}
						if (channel == 51) {
							System.out.println("hit 51 at " + tenth);

							Iterator<Entry<Integer, Integer>> it2 = channelMap.get(51).tailMap(start + 1).entrySet().iterator();
							it = channelMap.get(17).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap17, start, end, color);
							} else {
								setLightFcwWithRange(newMap17, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(19).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap19, start, end, color);
							} else {
								setLightFcwWithRange(newMap19, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(21).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap21, start, end, color);
							} else {
								setLightFcwWithRange(newMap21, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(23).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap23, start, end, color);
							} else {
								setLightFcwWithRange(newMap23, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(18).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap18, start, end, color);
							} else {
								setLightFcwWithRange(newMap18, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(20).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap20, start, end, color);
							} else {
								setLightFcwWithRange(newMap20, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);

							it = channelMap.get(22).tailMap(start + 1).entrySet().iterator();
							end = getEndTime(it, it2, start, color);
							end2 = getEndTime(it2, it, start, color);
							if(end <= end2) {
								setLightFcwWithRange(newMap22, start, end, color);
							} else {
								setLightFcwWithRange(newMap22, start, end2, color);
							}
							System.out.println("end: " + end + " end2: " + end2);
						}
                    }
					if (channel == 49) {
						channelMap.get(17).putAll(newMap17);
						channelMap.get(19).putAll(newMap19);
						channelMap.get(21).putAll(newMap21);
						channelMap.get(23).putAll(newMap23);
					}
					else if (channel == 50) {
						channelMap.get(18).putAll(newMap18);
						channelMap.get(20).putAll(newMap20);
						channelMap.get(22).putAll(newMap22);
					}
					else if (channel == 51) {
						channelMap.get(17).putAll(newMap17);
						channelMap.get(18).putAll(newMap18);
						channelMap.get(19).putAll(newMap19);
						channelMap.get(20).putAll(newMap20);
						channelMap.get(21).putAll(newMap21);
						channelMap.get(22).putAll(newMap22);
						channelMap.get(23).putAll(newMap23);
					}
                }
            } else{
                for (Integer tenth : channelMap.get(channel).keySet()) {
                    if (channelMap.get(channel).get(tenth) != 0) {
                        start = tenth;
                        color = channelMap.get(channel).get(tenth);
                        Iterator<Entry<Integer, Integer>> it = channelMap
                                .get(channel).tailMap(start + 1).entrySet()
                                .iterator();
                        while (it.hasNext()) {
                            Entry<Integer, Integer> timeColor = it.next();
                            if (timeColor.getValue() == 0
                                    && timeColor.getKey() != start) {
                                end = timeColor.getKey();
                                setLightFcwWithRange(newMap, start, end, color);
                                break;
                            } else if (timeColor.getValue() != color) {
                                end = timeColor.getKey();// - 1;
                                setLightFcwWithRange(newMap, start, end, color);
                                break;
                            }
                        }
                    }
                    channelMap.get(channel).putAll(newMap);
                }
            }
        }
    }

	public int getEndTime(Iterator<Entry<Integer, Integer>> it, Iterator<Entry<Integer, Integer>> it2, int start, int color) {
		int end = 0, end2 = 0;

		while (it.hasNext()) {
			Entry<Integer, Integer> timeColor = it.next();
			if (timeColor.getValue() == 0 && timeColor.getKey() != start) {
				end = timeColor.getKey();
				if(end >= start) {
					break;
				}
			} else if (timeColor.getValue() != color) {
				end = timeColor.getKey();// - 1;
				if(end >= start) {
					break;
				}
			}
		}
//		while (it2.hasNext()) {
//			Entry<Integer, Integer> timeColor = it2.next();
//			if (timeColor.getValue() == 0 && timeColor.getKey() != start) {
//				end2 = timeColor.getKey();
//				if(end2 >= start) {
//					break;
//				}
//			} else if (timeColor.getValue() != color) {
//				end2 = timeColor.getKey();// - 1;
//				if(end2 >= start) {
//					break;
//				}
//			}
//		}
//		System.out.println("end: " + end + " end2: " + end2);
//		if(end2 != 0 && end2 <= end) {
//			return end2;
//		}
		return end;
	}

	private void insertIntoTimeline( SortedMap<Integer, ArrayList<FCW>> srcTimeline, Integer i, FCW f) {
		if (srcTimeline.containsKey(i)) {
			for (FCW currentF : srcTimeline.get(i)) {
				if (currentF.equals(f)) {
				}
			}
			srcTimeline.get(i).add(f);
		} else {
			ArrayList<FCW> newFcw = new ArrayList<FCW>();
			newFcw.add(f);
			srcTimeline.put(i, newFcw);
		}
	}

	private void startAndEndPoints( SortedMap<Integer, SortedMap<Integer, Integer>> channelMap) {
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> lightTimeline = (ConcurrentSkipListMap<Integer, ArrayList<FCW>>) getLightTimeline();
		for (Integer timeIndex : lightTimeline.keySet()) {
			SortedMap<Integer, Integer> newMap = new ConcurrentSkipListMap<>();
			// int start = 0;
			for (FCW f : lightTimeline.get(timeIndex)) {
				// String name = FCWLib.getInstance().reverseLookupAddress(f);
				// String[] actions = FCWLib.getInstance().reverseLookupData(f);
				int color = f.getData();
				int tenthOfSec = timeIndex % 10;
				int secondsOnly = timeIndex / 10;
				double tenths = (double) tenthOfSec;
				double newTime = secondsOnly + (tenths / 10);
				int colAtTime = (int) (newTime * MusicPaneController
						.getInstance().getTimeFactor());
				if (colAtTime != 0) {
					colAtTime = colAtTime - 1;
				}
				if (color == 0) {
					// setLightFcwWithRange(newMap, start, timeIndex, f);
				}
				if (channelMap.containsKey(f.getAddr())) {
					channelMap.get(f.getAddr()).put(timeIndex, color);
					// start = timeIndex;
				} else {
					newMap.put(timeIndex, color);
					channelMap.putIfAbsent(f.getAddr(), newMap);
					// start = timeIndex;
				}
			}
			// [f.getAddr()] = data;
		}
	}

	private int countUChannels(SortedMap<Integer, ArrayList<FCW>> srcTimeline) {
		HashSet<Integer> address = new HashSet<>();

		for (ArrayList<FCW> a : srcTimeline.values()) {
			for (FCW f : a) {
				if (!address.contains(f.getAddr())) {
					address.add(f.getAddr());
				}
			}
		}

		return address.size();

	}

	// private void populateLightFcwArray() {

	public void sendTimelineInstanceToSliders(int time) {
		// if(waterTimeline.containsKey(time)) {
		int closestKey = 0;
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> waterTimeline = (ConcurrentSkipListMap<Integer, ArrayList<FCW>>) getWaterTimeline();
		if (time == 0) {
			closestKey = time;
		} else if (!waterTimeline.isEmpty()) {
			closestKey = waterTimeline.floorKey(time);
			// SlidersController.getInstance().setSlidersWithFcw(waterTimeline.get(closestKey));
			SlidersController.getInstance().setSlidersWithFcw(
					statefulTimeline.getStatefulTimelineMap().get(closestKey));

		}
		// }
	}

	public void sendTimelineInstanceToSim(int time) {
		// if(waterTimeline.containsKey(time)) {
		int closestKey = 0;
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> waterTimeline = (ConcurrentSkipListMap<Integer, ArrayList<FCW>>) getWaterTimeline();
		if (time == 0) {
			closestKey = time;
		} else if (!waterTimeline.isEmpty()) {
			closestKey = waterTimeline.floorKey(time);
			FountainSimController.getInstance().drawFcw(
					waterTimeline.get(closestKey));
		}
	}

	// public void updateColorsLists(int time){
	// for (int channel: channelColorMap.keySet()){
	// for()
	// }
	// FountainSimController.getInstance().getFrontCurtain().setFill(arg0);
	// }

	public boolean getActionsAtTime(int time) {
		ConcurrentSkipListMap<Integer, ArrayList<FCW>> waterTimeline = (ConcurrentSkipListMap<Integer, ArrayList<FCW>>) getWaterTimeline();
		return waterTimeline.containsKey(time);
	}

	public void sendSubmapToSim(int tenthsTime) {
		// FountainSimController.getInstance().acceptSubmapOfFcws(timeline.tailMap(timeline.floorKey(tenthsTime)));
		// FountainSimController.getInstance().acceptSubmapOfFcws(timeline.subMap(tenthsTime,
		// true, MusicPaneController.getInstance().SONG_TIME, true));
	}

	private boolean checkForCollision( SortedMap<Integer, ArrayList<FCW>> timeline, int pointInTime, FCW query) {
		boolean result = false;
		if (timeline.containsKey(pointInTime)) {
			for (FCW f : timeline.get(pointInTime)) {
				if (f.getAddr() == query.getAddr()) {

					String[] fActions = FCWLib.getInstance().reverseLookupData(
							f);
					String[] queryActions = FCWLib.getInstance()
							.reverseLookupData(query);
					for (String fAction : fActions) {
						for (String queryAction : queryActions) {
							if (fAction.equals(queryAction)
									&& !FCWLib.getInstance().isLevel(fAction)) {
								// query.setData(query.getData() + f.getData());
								return true;
							}
							// else if(!fAction.equals(queryAction) &&
							// !FCWLib.getInstance().isLevel(fAction)) {
							// if(f.getData() > query.getData()) {
							// query.setData(f.getData() - query.getData());
							// }
							// else {
							// query.setData(f.getData() + query.getData());
							// }
							// }
						}
					}
					if (f.getAddr() == 54) {
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	// Should be deleteWaterAtTime, as it doesnt work with other timeline...?
	public void deleteActionAtTime(int i) {
		ArrayList<FCW> fcws = timeline.get(new Integer(i));
		for (FCW action : fcws) {
			if (action.getIsWater()) {
				fcws.remove(action);
			}
		}
		timeline.replace(new Integer(i), fcws);
		// waterTimeline.remove(i);
	}

	/*
	 * public void collapseTimelines() { ConcurrentSkipListMap<Integer,
	 * ArrayList<FCW>> result = new ConcurrentSkipListMap<>(); //
	 * timeline.clear(); insertFcwsIntoTimeline(lightTimeline);
	 * insertFcwsIntoTimeline(waterTimeline); timeline = result; }
	 */

	public void insertFcwsIntoTimeline(
			SortedMap<Integer, ArrayList<FCW>> srcTimeline) {
		for (Integer timeIndex : srcTimeline.keySet()) {
			for (FCW f : srcTimeline.get(timeIndex)) {
				insertIntoTimeline(timeline, timeIndex, f);
			}
		}
	}
}
