package data;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 *
 * @author ToanHo
 */
public class ArpeggiatorBlock extends Block {

	private String arpPattern;
	private int resolutionTickPerBeat;

	public ArpeggiatorBlock(String arpPattern, List<Byte> listOfNoteValues) {
		type = AppConstant.ARPEGGIATOR;
		this.listOfNoteValues = listOfNoteValues;
		this.setArpPattern(arpPattern);
		this.resolutionTickPerBeat = AppConstant.DEFAULT_RESOLUTION_TICKS_PER_BEAT;
	}

	@Override
	public Sequence process(Sequence rawSequence) {
		// TODO
		Sequence resultSequence = null;
		try {
			resultSequence = new Sequence(rawSequence.getDivisionType(), rawSequence.getResolution());
			for (Track rawTrack : rawSequence.getTracks()) {
				Track resultTrack = resultSequence.createTrack();
				List<ShortMessage> noteOnList = new ArrayList<ShortMessage>();
				long lastTick = 0;
				boolean isFirst = true;
				for (int i = 0; i < rawTrack.size(); i++) {
					MidiEvent midiEvent = rawTrack.get(i);
					MidiMessage midiMessage = midiEvent.getMessage();
					if (midiMessage instanceof ShortMessage) {
						ShortMessage shortMessage = (ShortMessage) midiMessage;
						int command = shortMessage.getCommand();
						long currentTick = midiEvent.getTick();
						if (command == ShortMessage.NOTE_ON) {
							if (isFirst == false) {
								List<ShortMessage> arpList = createArpList(noteOnList, lastTick, currentTick);
								for (int k = 0; k < arpList.size() - 1; k++) {
									// Add NOTE_ON message
									ShortMessage message = arpList.get(k);
									MidiEvent resultMidiEvent = new MidiEvent(message, lastTick);
									resultTrack.add(resultMidiEvent);
									// Add NOTE_OFF message
									lastTick = lastTick + resolutionTickPerBeat;
									ShortMessage offShortMessage = createNoteOffMessage(message);
									MidiEvent offMidiEvent = new MidiEvent(offShortMessage, lastTick);
									resultTrack.add(offMidiEvent);
								}
								if (currentTick > lastTick && arpList.size() > 0) {
									ShortMessage message = arpList.get(arpList.size() - 1);
									MidiEvent resultMidiEvent = new MidiEvent(message, lastTick);
									resultTrack.add(resultMidiEvent);
									lastTick = currentTick;
									ShortMessage offShortMessage = createNoteOffMessage(message);
									MidiEvent offMidiEvent = new MidiEvent(offShortMessage, lastTick);
									resultTrack.add(offMidiEvent);
								}
							} else {
								isFirst = false;
							}
							noteOnList.add(shortMessage);
						} else if (command == ShortMessage.NOTE_OFF) {
							List<ShortMessage> arpList = createArpList(noteOnList, lastTick, currentTick);
							for (int k = 0; k < arpList.size() - 1; k++) {
								// Add NOTE_ON message
								ShortMessage message = arpList.get(k);
								MidiEvent resultMidiEvent = new MidiEvent(message, lastTick);
								resultTrack.add(resultMidiEvent);
								// Add NOTE_OFF message
								lastTick = lastTick + resolutionTickPerBeat;
								ShortMessage offShortMessage = createNoteOffMessage(message);
								MidiEvent offMidiEvent = new MidiEvent(offShortMessage, lastTick);
								resultTrack.add(offMidiEvent);
							}
							if (currentTick > lastTick) {
								ShortMessage message = arpList.get(arpList.size() - 1);
								MidiEvent resultMidiEvent = new MidiEvent(message, lastTick);
								resultTrack.add(resultMidiEvent);
								lastTick = currentTick;
								ShortMessage offShortMessage = createNoteOffMessage(message);
								MidiEvent offMidiEvent = new MidiEvent(offShortMessage, lastTick);
								resultTrack.add(offMidiEvent);
							}
							noteOnList = removeNoteOnMessage(noteOnList, shortMessage);
						}
					} else {
						MidiEvent otherMidiEvent = new MidiEvent(midiMessage, midiEvent.getTick());
						resultTrack.add(otherMidiEvent);
					}
				}
			}
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
		return resultSequence;
	}

	/**
	 * Create NOTE_OFF message based on the given NOTE_ON message
	 * 
	 * @param noteOnMessage
	 * @return
	 */
	private ShortMessage createNoteOffMessage(ShortMessage noteOnMessage) {
		int data1 = noteOnMessage.getData1();
		int data2 = noteOnMessage.getData2();
		int channel = noteOnMessage.getChannel();
		ShortMessage offShortMessage = null;
		try {
			offShortMessage = new ShortMessage(ShortMessage.NOTE_OFF, channel, data1, data2);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
		return offShortMessage;
	}

	/**
	 * Create a order arpeggiator list
	 * 
	 * @param noteOnList
	 * @param startTick
	 * @param endTick
	 * @return
	 */
	private List<ShortMessage> createOrderList(List<ShortMessage> noteOnList, long startTick, long endTick) {
		long duration = endTick - startTick;
		List<ShortMessage> resultList = new ArrayList<ShortMessage>();
		long numberOfNoteEvent = duration / resolutionTickPerBeat;
		for (int i = 0, j = 0; i <= numberOfNoteEvent; i++, j++) {
			if (j == noteOnList.size()) {
				j = 0;
			}
			resultList.add(noteOnList.get(j));
		}
		return resultList;
	}

	/**
	 * Create ping pong list
	 * 
	 * @param noteOnList
	 * @param startTick
	 * @param endTick
	 * @return
	 */
	private List<ShortMessage> createPingPongList(List<ShortMessage> noteOnList, long startTick, long endTick) {
		long duration = endTick - startTick;
		List<ShortMessage> resultList = new ArrayList<ShortMessage>();
		Collections.sort(noteOnList, SHORT_MESSAGE_COMPARATOR);
		long numberOfNoteEvent = duration / resolutionTickPerBeat;
		int step = 1;
		if (noteOnList.size() >= 2) {
			for (int i = 0, j = 0; i <= numberOfNoteEvent; i++) {
				if (j == noteOnList.size()) {
					j = noteOnList.size() - 2;
					step = -1;
				}
				if (j == -1) {
					j = 1;
					step = 1;
				}
				resultList.add(noteOnList.get(j));
				j = j + step;
			}
		} else if (noteOnList.size() == 1) {
			for (int i = 0; i <= numberOfNoteEvent; i++) {
				resultList.add(noteOnList.get(0));
			}
		}
		return resultList;
	}

	/**
	 * Create random arpeggiator list
	 * 
	 * @param noteOnList
	 * @param startTick
	 * @param endTick
	 * @return
	 */
	private List<ShortMessage> createRandomList(List<ShortMessage> noteOnList, long startTick, long endTick) {
		long duration = endTick - startTick;
		Random random = new Random();
		int lastData1 = Integer.MIN_VALUE;
		List<ShortMessage> resultList = new ArrayList<ShortMessage>();
		long numberOfNoteEvent = duration / resolutionTickPerBeat;
		for (int i = 0; i <= numberOfNoteEvent; i++) {
			int index = random.nextInt(noteOnList.size());
			ShortMessage message = noteOnList.get(index);
			if (noteOnList.size() > 1) {
				if (lastData1 == Integer.MIN_VALUE) { // The first element
					resultList.add(message);
					lastData1 = message.getData1();
				} else {
					while (message.getData1() == lastData1) {
						index = random.nextInt(noteOnList.size());
						message = noteOnList.get(index);
					}
					resultList.add(message);
					lastData1 = message.getData1();
				}
			} else { // size = 1
				resultList.add(message);
			}
		}
		return resultList;
	}

	public String getArpPattern() {
		return arpPattern;
	}

	public void setArpPattern(String arpPattern) {
		this.arpPattern = arpPattern;
	}

	/**
	 * Remove a specific short message from the given list of short messages
	 * 
	 * @param noteOnList
	 * @param message
	 * @return
	 */
	public List<ShortMessage> removeNoteOnMessage(List<ShortMessage> noteOnList, ShortMessage message) {
		int data1 = message.getData1();
		int index = -1;
		for (int i = 0; i < noteOnList.size(); i++) {
			if (data1 == noteOnList.get(i).getData1()) {
				index = i;
				break;
			}
		}
		if (index != -1) {
			noteOnList.remove(index);
		}
		return noteOnList;
	}

	@Override
	public String toString() {
		return type + AppConstant.SEPARATOR + arpPattern;
	}

	public static final Comparator<ShortMessage> SHORT_MESSAGE_COMPARATOR = new Comparator<ShortMessage>() {
		@Override
		public int compare(ShortMessage o1, ShortMessage o2) {
			// TODO Auto-generated method stub
			ShortMessage message1 = (ShortMessage) o1;
			ShortMessage message2 = (ShortMessage) o2;
			if (message1.getData1() > message2.getData1()) {
				return 1;
			} else if (message1.getData1() == message2.getData1()) {
				return 0;
			} else {
				return -1;
			}
		}
	};

	/**
	 * Create arpeggiator list based on list of currently ON note
	 * 
	 * @param noteOnList
	 * @param startTick
	 * @param endTick
	 */
	public List<ShortMessage> createArpList(List<ShortMessage> noteOnList, long startTick, long endTick) {
		List<ShortMessage> arpList = new ArrayList<ShortMessage>();
		if (noteOnList.size() > 0) {
			if (arpPattern == AppConstant.ASCENDING_SCALE) {
				Collections.sort(noteOnList, SHORT_MESSAGE_COMPARATOR);
				arpList = createOrderList(noteOnList, startTick, endTick);
			} else if (arpPattern == AppConstant.DESCENDING_SCALE) {
				Collections.sort(noteOnList, SHORT_MESSAGE_COMPARATOR);
				Collections.reverse(noteOnList);
				arpList = createOrderList(noteOnList, startTick, endTick);
			} else if (arpPattern == AppConstant.PING_PONG) {
				arpList = createPingPongList(noteOnList, startTick, endTick);
			} else { // arpPattern == AppConstant.RANDOM
				arpList = createRandomList(noteOnList, startTick, endTick);
			}
		}
		return arpList;
	}

}
