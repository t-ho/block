package data;


import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.jfugue.theory.Note;
import org.staccato.StaccatoUtil;

/**
 *
 * @author ToanHo
 */
public class ChordifyBlock extends Block {

	public ChordifyBlock(List<Byte> listOfNoteValues) {
		type = AppConstant.CHORDIFY;
		this.listOfNoteValues = listOfNoteValues;
	}

	@Override
	public Sequence process(Sequence rawSequence) {
		Sequence resultSequence = null;
		try {
			resultSequence = new Sequence(rawSequence.getDivisionType(), rawSequence.getResolution());
			for (Track rawTrack : rawSequence.getTracks()) {
				Track resultTrack = resultSequence.createTrack();
				for (int i = 0; i < rawTrack.size(); i++) {
					MidiEvent midiEvent = rawTrack.get(i);
					MidiMessage message = midiEvent.getMessage();
					if (message instanceof ShortMessage) {
						ShortMessage shortMessage = (ShortMessage) message;
						byte noteValue = (byte) shortMessage.getData1();
						List<Integer> data = getChordNotes(noteValue);
						int command = shortMessage.getCommand();
						int channel = shortMessage.getChannel();
						int data2 = shortMessage.getData2();
						for (int j = 0; j < data.size(); j++) {
							ShortMessage resultShortMessage = new ShortMessage(command, channel, data.get(j), data2);
							MidiEvent resultMidiEvent = new MidiEvent(resultShortMessage, midiEvent.getTick());
							resultTrack.add(resultMidiEvent);
						}
					} else {
						MidiEvent otherMidiEvent = new MidiEvent(message, midiEvent.getTick());
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
	 * Get chord notes
	 * @param noteValue
	 * @return list of chord notes
	 */
	private List<Integer> getChordNotes(byte noteValue) {
		List<Integer> data = new ArrayList<Integer>();
		int currentIndex = listOfNoteValues.indexOf(noteValue);
		if (currentIndex != -1) {
			data.add((int) noteValue);
			int nextIndex1 = currentIndex + 2;
			if (nextIndex1 < listOfNoteValues.size()) {
				data.add((int) listOfNoteValues.get(nextIndex1));
				int nextIndex2 = currentIndex + 4;
				if (nextIndex2 < listOfNoteValues.size()) {
					data.add((int) listOfNoteValues.get(nextIndex2));
				}
			}
		} else {
			//TODO if the current note is not in the current set scale
		}
		return data;
	}

	@Override
	public String toString() {
		return type;
	}
}
