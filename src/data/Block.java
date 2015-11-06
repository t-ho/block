package data;

import java.io.Serializable;
import java.util.List;

import javax.sound.midi.Sequence;

import org.jfugue.parser.ParserListener;
import org.jfugue.pattern.Pattern;
import org.jfugue.pattern.PatternProducer;
import org.jfugue.theory.Chord;
import org.jfugue.theory.Note;
import org.staccato.StaccatoParser;
import org.staccato.StaccatoParserListener;
import org.staccato.StaccatoUtil;

/**
 *
 * @author ToanHo
 */
public abstract class Block implements Serializable {

	protected String type;
	protected List<Byte> listOfNoteValues;

	public abstract Sequence process(Sequence rawSequence);

	public String getType() {
		return type;
	}

	public String toString() {
		return type;
	}

	public void setListOfNoteValues(List<Byte> listOfNoteValues) {
		this.listOfNoteValues = listOfNoteValues;
	}
}
