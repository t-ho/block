package data;


import java.util.ArrayList;
import java.util.List;

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
public class GateBlock extends Block {

	private float notesPerTick;
	private String mode;
	long resolutionTickPerBeat;
	private List<ShortMessage> shortMessageList;
	private List<ShortMessage> noteOnOutputList;

	public GateBlock(String mode, float notesPerTick) {
		type = AppConstant.GATE;
		this.setMode(mode);
		this.setNotesPerTick(notesPerTick);
		this.resolutionTickPerBeat = AppConstant.DEFAULT_RESOLUTION_TICKS_PER_BEAT;
		this.shortMessageList = new ArrayList<ShortMessage>();
		this.noteOnOutputList = new ArrayList<ShortMessage>();
	}

	public float getNotesPerTick() {
		return notesPerTick;
	}

	public void setNotesPerTick(float notesPerTick) {
		this.notesPerTick = notesPerTick;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		String result = type + AppConstant.SEPARATOR + mode + AppConstant.SEPARATOR + Float.toString(notesPerTick);
		return result;
	}

	@Override
	public Sequence process(Sequence rawSequence) {
		// TODO Auto-generated method stub
		long closingDuration = resolutionTickPerBeat;
		int numberOfNote = 0;
		if (notesPerTick < 1) {
			numberOfNote = 1;
			closingDuration = (long) Math.ceil(1 / notesPerTick) * resolutionTickPerBeat;
		} else { // notesPerTick >= 1
			numberOfNote = Math.round(notesPerTick);
			closingDuration = resolutionTickPerBeat;
		}
//		System.out.println("\nnumber of notes: " + numberOfNote);
//		System.out.println("duration: " + closingDuration);

		Sequence resultSequence = null;
		try {
			resultSequence = new Sequence(rawSequence.getDivisionType(), rawSequence.getResolution());
			for (Track rawTrack : rawSequence.getTracks()) {
				Track resultTrack = resultSequence.createTrack();
				shortMessageList = new ArrayList<ShortMessage>();
				long startTick = -1;
				long endTick = 0;
				long lastTick = SequenceUtil.getLastTick(rawSequence);
				boolean isContinue = true;
				while (isContinue) {

//					System.out.println("\n Start: " + startTick + " " + endTick);
					updateShortMessageList(rawTrack, startTick, endTick);
					if (endTick > lastTick && shortMessageList.size() == 0) {
						isContinue = false;
					}
					resultTrack = releaseNote(resultTrack, numberOfNote, endTick);
					startTick = endTick;
					endTick = endTick + closingDuration;

				}
			}
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
		resultSequence = SequenceUtil.setTempoBPM(resultSequence, SequenceUtil.getTempoBPM(rawSequence));
		return resultSequence;
	}

	private void updateShortMessageList(Track rawTrack, long startTick, long endTick) {
		// TODO Auto-generated method stub
		for (int i = 0; i < rawTrack.size(); i++) {
			MidiEvent midiEvent = rawTrack.get(i);
			MidiMessage midiMessage = midiEvent.getMessage();
			if (midiMessage instanceof ShortMessage) {
				ShortMessage shortMessage = (ShortMessage) midiMessage;
				long currentTick = midiEvent.getTick();
				if (currentTick <= endTick && currentTick > startTick) {
					shortMessageList.add(shortMessage);
				}
			}
		}
//		System.out.println("List size: " + shortMessageList.size());
	}

	private Track releaseNote(Track track, int numberOfNotes, long currentTick) {
		// TODO Auto-generated method stub
		shortMessageList = cancelPairNoteOnOff(shortMessageList);
		int n = (numberOfNotes < shortMessageList.size()) ? numberOfNotes : shortMessageList.size();
//		System.out.println(numberOfNotes + " and " + shortMessageList.size() + " n = " + n);
		if (mode == AppConstant.QUEUE) {
			int first = 0;
			for (int i = 0; i < n; i++) {
				ShortMessage message = shortMessageList.get(first);
				MidiEvent midiEvent = new MidiEvent(message, currentTick);
				track.add(midiEvent);
				shortMessageList.remove(first);
			}
		} else if (mode == AppConstant.FIRST_HOLD) {
			// Release all the NOTE_OFF in the current queue
			for (int i = 0; i < shortMessageList.size(); i++) {
				ShortMessage message = shortMessageList.get(i);
				int command = message.getCommand();
				if (command == ShortMessage.NOTE_OFF) {
					MidiEvent midiEvent = new MidiEvent(message, currentTick);
					int data1 = message.getData1();
					for (int j = 0; j < noteOnOutputList.size(); j++) {
						if (data1 == noteOnOutputList.get(j).getData1()) {
							track.add(midiEvent);
							noteOnOutputList.remove(j);
						}
					}
					shortMessageList.remove(i);
				}
			}
			n = (numberOfNotes < shortMessageList.size()) ? numberOfNotes : shortMessageList.size();
			for (int i = 0; i < n; i++) {
				int first = 0;
				ShortMessage message = shortMessageList.get(first);
				MidiEvent midiEvent = new MidiEvent(message, currentTick);
				int command = message.getCommand();
				if (command == ShortMessage.NOTE_ON) {
					track.add(midiEvent);
					noteOnOutputList.add(message);
//					System.out.println("In First_hold, add Note ON_gate");
				} else { // NOTE_OFF
					int data1 = message.getData1();
					for (int j = 0; j < noteOnOutputList.size(); j++) {
						if (data1 == noteOnOutputList.get(j).getData1()) {
							track.add(midiEvent);
							noteOnOutputList.remove(j);
//							System.out.println("In First_hold, pass NOTE_OFF based on OnOutPut");
						}
					}
				}
				shortMessageList.remove(first);
			}
			shortMessageList.clear();
		} else { // LAST_HOLD
			// Release all the NOTE_OFF in the current queue
			for (int i = 0; i < shortMessageList.size(); i++) {
				ShortMessage message = shortMessageList.get(i);
				int command = message.getCommand();
				if (command == ShortMessage.NOTE_OFF) {
					MidiEvent midiEvent = new MidiEvent(message, currentTick);
					int data1 = message.getData1();
					for (int j = 0; j < noteOnOutputList.size(); j++) {
						if (data1 == noteOnOutputList.get(j).getData1()) {
							track.add(midiEvent);
							noteOnOutputList.remove(j);
						}
					}
					shortMessageList.remove(i);
				}
			} 
			n = (numberOfNotes < shortMessageList.size()) ? numberOfNotes : shortMessageList.size();
			for (int i = 0; i < n; i++) {
				int last = shortMessageList.size() - 1;
				ShortMessage message = shortMessageList.get(last);
				MidiEvent midiEvent = new MidiEvent(message, currentTick);
				int command = message.getCommand();
				if (command == ShortMessage.NOTE_ON) {
					track.add(midiEvent);
					noteOnOutputList.add(message);
				} else { // NOTE_OFF
					int data1 = message.getData1();
					for (int j = 0; j < noteOnOutputList.size(); j++) {
						if (data1 == noteOnOutputList.get(j).getData1()) {
							track.add(midiEvent);
							noteOnOutputList.remove(j);
						}
					}
				}
				shortMessageList.remove(last);
			}
			shortMessageList.clear();
		}
		return track;
	}

	/**
	 * Drop NOTE_ON and NOTE_OFF of MIDI signal in the same queue
	 * 
	 * @param shortMessageList
	 * @return
	 */
	private List<ShortMessage> cancelPairNoteOnOff(List<ShortMessage> shortMessageList) {
		// TODO Auto-generated method stub
		List<ShortMessage> resultList = new ArrayList<ShortMessage>();
		List<Integer> deleteIndexes = new ArrayList<Integer>();
		if (shortMessageList.size() <= 1) {
			return shortMessageList;
		}
		boolean deleteLast = false;
		for (int i = 0; i < shortMessageList.size() - 1; i++) {
			if(deleteIndexes.indexOf(i) != -1) {
				break;
			}
			int j = 0;
			for (j = i + 1; j < shortMessageList.size(); j++) {
				if (shortMessageList.get(i).getData1() == shortMessageList.get(j).getData1()) {
					if (j == shortMessageList.size() - 1) {
						deleteLast = true;
					}
					deleteIndexes.add(j);
					break;
				}
			}
			if (j == shortMessageList.size()) {
				resultList.add(shortMessageList.get(i));
			}
		}
		if (deleteLast == false) {
			resultList.add(shortMessageList.get(shortMessageList.size() - 1));
		}
		return resultList;
	}

}
