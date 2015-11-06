package data;


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
public class PitchShiftBlock extends Block {

	private int numberOfNotes;

	public PitchShiftBlock(int numberOfNotes, List<Byte> listOfNoteValues) {
		super();
		this.type = AppConstant.PITCH_SHIFT;
		this.listOfNoteValues = listOfNoteValues;
		this.numberOfNotes = numberOfNotes;
	}
	

//	private Note shift(Note note) {
////		System.out.println(note.getValue());
////		System.out.println(note.getDuration());
////		System.out.println(note.isDurationExplicitlySet());
////		System.out.println(note.getOnVelocity());
////		System.out.println(note.getOffVelocity());
////		System.out.println(note.isRest());
////		System.out.println(note.isStartOfTie());
//		
//		Note shiftedNote = new Note(note);
//		int currentIndex = getRoundedIndex(note.getValue());
//		int shiftedIndex = currentIndex + numberOfNotes;
//		if (shiftedIndex < 0) {
//			shiftedIndex = 0;
//		} else if (shiftedIndex >= listOfNoteValues.size()) {
//			shiftedIndex = listOfNoteValues.size() - 1;
//		}
//		shiftedNote.setValue(listOfNoteValues.get(shiftedIndex));
//		shiftedNote.setOriginalString(Note.getToneString(shiftedNote.getValue()));
//		
////		System.out.println(note.getValue());
//		return shiftedNote;
//	}

	/**
	 * Get rounded index
	 * @param value
	 * @return
	 */
	private int getRoundedIndex(int value) {
		int round[] = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6};
//		for(int i = 0; i < listOfNoteValues.size(); i++) {
//			System.out.print(listOfNoteValues.get(i) + " ");
//		}
		for(int i = 0; i < round.length; i++) {
			int roundedValue = value + round[i];
//			System.out.println("\n" + roundedValue);
			int index = listOfNoteValues.indexOf((byte) roundedValue);
//			System.out.println(index);
			if(index != -1) {
				return index;
			}
		}
		if(value < listOfNoteValues.get(0)) {
			return 0;
		} else {
			return listOfNoteValues.size() - 1;
		}
	}
	
	public int getNumberOfNotes() {
		return numberOfNotes;
	}

	public void setNumberOfNotes(int numberOfNotes) {
		this.numberOfNotes = numberOfNotes;
	}

	@Override
	public String toString() {
		String result = this.type + AppConstant.SEPARATOR + Integer.toString(numberOfNotes);
		return result;
	}
	
	@Override
	public Sequence process(Sequence rawSequence) {
		Sequence resultSequence = null;
		try {
			resultSequence = new Sequence(rawSequence.getDivisionType(), rawSequence.getResolution());
			for(Track rawTrack : rawSequence.getTracks()) {
				Track resultTrack = resultSequence.createTrack();
				for(int i = 0; i < rawTrack.size(); i++) {
					MidiEvent midiEvent = rawTrack.get(i);
					MidiMessage message = midiEvent.getMessage();
					if(message instanceof ShortMessage) {
						ShortMessage shortMessage = (ShortMessage) message;
						byte noteValue = (byte) shortMessage.getData1();
						int data1 = shiftNote(noteValue);
						int command = shortMessage.getCommand();
						int channel =  shortMessage.getChannel();
						int data2 =  shortMessage.getData2();
						ShortMessage resultShortMessage = new ShortMessage(command, channel, data1, data2);
						MidiEvent resultMidiEvent = new MidiEvent(resultShortMessage, midiEvent.getTick());
						resultTrack.add(resultMidiEvent);
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

	private int shiftNote(byte noteValue) {
		int currentIndex = getRoundedIndex(noteValue);
		int shiftedIndex = currentIndex + numberOfNotes;
		if (shiftedIndex < 0) {
			shiftedIndex = 0;
		} else if (shiftedIndex >= listOfNoteValues.size()) {
			shiftedIndex = listOfNoteValues.size() - 1;
		}
		int shiftedNoteValue = (int) listOfNoteValues.get(shiftedIndex);
		return shiftedNoteValue;
	}
	
}
