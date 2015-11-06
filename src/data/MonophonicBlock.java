package data;


import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.jfugue.midi.MidiDefaults;

/**
 *
 * @author ToanHo
 */
public class MonophonicBlock extends Block {

	public MonophonicBlock(List<Byte> listOfNoteValues) {
		type = AppConstant.MONOPHONIC;
		this.listOfNoteValues = listOfNoteValues;
	}

	@Override
	public Sequence process(Sequence rawSequence) {
		Sequence resultSequence = null;
		try {
			resultSequence = new Sequence(rawSequence.getDivisionType(), rawSequence.getResolution());
			for (Track rawTrack : rawSequence.getTracks()) {
				Track resultTrack = resultSequence.createTrack();
				boolean hasLastNoteOn = false;
				ShortMessage lastShortMessage = new ShortMessage();
				for (int i = 0; i < rawTrack.size(); i++) {
					MidiEvent midiEvent = rawTrack.get(i);
					MidiMessage message = midiEvent.getMessage();
					if (message instanceof ShortMessage) {
						ShortMessage shortMessage = (ShortMessage) message;
						int command = shortMessage.getCommand();
						int data1 = shortMessage.getData1();
						int channel = shortMessage.getChannel();
						int data2 = shortMessage.getData2();
						if (command == ShortMessage.NOTE_ON) {
							if (hasLastNoteOn == true) {
								ShortMessage offShortMessage = new ShortMessage(ShortMessage.NOTE_OFF, channel,
										lastShortMessage.getData1(), lastShortMessage.getData2());
								System.out.println(midiEvent.getTick());
								MidiEvent offMidiEvent = new MidiEvent(offShortMessage, midiEvent.getTick());
								resultTrack.add(offMidiEvent);
							}
							ShortMessage resultShortMessage = new ShortMessage(command, channel, data1, data2);
							MidiEvent resultMidiEvent = new MidiEvent(resultShortMessage, midiEvent.getTick());
							resultTrack.add(resultMidiEvent);
							hasLastNoteOn = true;
							lastShortMessage = resultShortMessage;
						} else if (command == ShortMessage.NOTE_OFF) {
							if (data1 == lastShortMessage.getData1()) {
								ShortMessage resultShortMessage = new ShortMessage(command, channel, data1, data2);
								MidiEvent resultMidiEvent = new MidiEvent(resultShortMessage, midiEvent.getTick());
								resultTrack.add(resultMidiEvent);
								hasLastNoteOn = false;
								lastShortMessage = new ShortMessage();
							}
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

	@Override
	public String toString() {
		return type;
	}

}
