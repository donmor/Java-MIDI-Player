
package indi.donmor.midiplayer;

import java.io.File;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.MidiDevice.Info;

public class MIDICore {

	public int devID, devDiv;

	private MidiDevice midid;

	private Sequencer midip;

	private Sequence sequence;

	private Info[] devs;

	public String[] devx;

	public long midiPauseProg, midiLoopStart, midiLoopEnd;

	public enum cycleType {
		none, whole, partial
	}

	public cycleType repeat;

	public MIDICore(int devi) {

		repeat = cycleType.none;
		devs = MidiSystem.getMidiDeviceInfo();
		// devx = new String[devs.length];
		String[] src = new String[0];
		for (int i = 0; i < devs.length; i++) {
			String s = devs[i].toString();
			if (s != "Real Time Sequencer") {
				if (s == "Gervill")
					s = "Internal";
				String[] dest = new String[src.length + 1];
				System.arraycopy(src, 0, dest, 0, src.length);
				dest[src.length] = s;
				src = dest;
			} else {
				devDiv = i;
				// System.out.println(i);
			}
		}
		devx = src;
		devID = devFix(devi);
		try {
			try {
				midid = MidiSystem.getMidiDevice(devs[devID]);
			} catch (Exception e) {
				// System.out.println(e);
				devID = 0;
				midid = MidiSystem.getMidiDevice(devs[devID]);
			}
			midid.open();
			midip = MidiSystem.getSequencer(false);
			midip.open();
			midip.getTransmitter().setReceiver(midid.getReceiver());
		} catch (Exception e) {
			// System.out.println(e);
		}
	}

	public void changeDev(int id) {

		midiPauseProg = midip.getMicrosecondPosition();
		long pauseProg = midiPauseProg;
		// System.out.println(midiPauseProg);
		devID = devFix(id);
		boolean running = midip.isRunning();
		try {
			shutdown();
			midid = MidiSystem.getMidiDevice(devs[devID]);
			midid.open();
			midip = MidiSystem.getSequencer(false);
			midip.open();
			midip.getTransmitter().setReceiver(midid.getReceiver());
			midip.setSequence(sequence);
			changeCycleMethod();
			if (running) {
				midip.start();
			}

		} catch (Exception e) {
			// System.out.println(e);
		}
		midiPauseProg = pauseProg;
		// System.out.println(midiPauseProg);
		midip.setMicrosecondPosition(midiPauseProg);
	}

	public long getMidiProg() {

		return midip.getMicrosecondPosition();
	}

	public long getMidiTickProg() {

		return midip.getTickPosition();
	}

	public void changeMidi(File file, boolean playNow) {

		try {
			sequence = MidiSystem.getSequence(file);
			midip.setSequence(sequence);
			changeCycleMethod();
			midiPauseProg = 0;
			if (playNow)
				midip.start();
		} catch (Exception e) {

		}
	}

	public void shutdown() {

		midip.close();
		midid.close();
	}

	public boolean togglePause() {

		boolean i = false;
		try {
			if (isPlaying()) {
				midiPauseProg = midip.getMicrosecondPosition();
				midip.stop();
			} else {
				midip.start();
				midip.setMicrosecondPosition(midiPauseProg);
			}
			i = true;
		} catch (Exception e) {
			if (e instanceof java.lang.IllegalStateException) {
				i = false;
			}
		}
		return i;

	}

	public void midiStop() {

		if (isPlaying())
			midip.stop();
		midip.setMicrosecondPosition(0);
		midiPauseProg = 0;
	}

	public Boolean isPlaying() {

		return midip.isRunning();
	}

	public long getLength() {

		return midip.getMicrosecondLength();
	}

	public cycleType changeCycleMethod() {

		switch (repeat) {
		case none:
			midip.setLoopCount(0);
			break;
		case whole:
			midip.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
			midip.setLoopStartPoint(0);
			midip.setLoopEndPoint(-1);
			break;
		case partial:
			midip.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
			float fps = sequence.getDivisionType();
			// System.out.println(fps);
			try {
				if (midiLoopStart < midiLoopEnd) {
					if (fps == Sequence.PPQ) {
						midip.setLoopStartPoint((long) (midiLoopStart
								* midip.getTempoInBPM()
								* sequence.getResolution() / 60000000));
						midip.setLoopEndPoint((long) (midiLoopEnd
								* midip.getTempoInBPM()
								* sequence.getResolution() / 60000000));
					} else if (fps > Sequence.PPQ) {
						midip.setLoopStartPoint((long) (midiLoopStart * fps
								* sequence.getResolution() / 1000000));
						midip.setLoopEndPoint((long) (midiLoopEnd * fps
								* sequence.getResolution() / 1000000));
					} else {
						throw new Exception();
					}
				} else {
					throw new Exception();
				}
			} catch (Exception e) {
				repeat = cycleType.none;
			}
			break;
		default:
			break;
		}
		return repeat;
	}

	public void midiNavigate(long time) {

		if (!((repeat == cycleType.partial && time >= midiLoopEnd) | time >= midip
				.getMicrosecondLength())) {
			if (time >= 0) {
				midip.setMicrosecondPosition(time);
				midiPauseProg = time;
			} else {
				midip.setMicrosecondPosition(0);
				midiPauseProg = 0;
			}
		}

		// else
		// {
		// midip.setMicrosecondPosition(0);
		// midiPauseProg = 0;
		// }

	}

	private int devFix(int id) {

		if (id >= devDiv)
			return id + 1;
		else
			return id;
	}
}

