
package indi.donmor.midiplayer;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import indi.donmor.midiplayer.MIDICore;
import indi.donmor.midiplayer.MIDICore.cycleType;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JSlider;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileFilter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MIDIplayer extends JFrame {

	private static File midiFile;

	private static File lastDirectory;

	private static MIDICore core;

	private static final long serialVersionUID = 1L;

	private static boolean isDragging, isKeySeeking;

	private static boolean doFileOpened;

	private JPanel contentPane;

	private static JTextField fName;

	private static JButton btnPlaypause, btnStop, btnRew, btnFf;

	private static JToggleButton tglbtnRepeat;

	private static JList<String> list;

	private static JTextField currentTime, totalTime, txtCurrentdev;

	private static JSlider slider;

	private static Toolkit toolkit;

	private static Image icon;

	{

	}

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {

		EventQueue.invokeLater(new Runnable() {

			public void run() {

				Properties prop = new Properties();
				toolkit = Toolkit.getDefaultToolkit();
				icon = toolkit.createImage(getClass().getResource("/resources/images/midi.gif"));
				File vLd = null;
				boolean vRep = false;
				int vDev = 0;
				int vX = (toolkit.getScreenSize().width - 600) / 2;
				int vY = (toolkit.getScreenSize().height - 400) / 2;
				try {
					File propFile = new File("midiplayer.properties");
					if (!propFile.exists()) {
						throw new IOException();
					}
					FileInputStream propInputStream = new FileInputStream(propFile);
					prop.load(propInputStream);
					vRep = Boolean.valueOf(prop.getProperty("repeat"));
					try {
						vLd = new File(prop.getProperty("last-directory"));
					} catch (Exception e) {
					}
					try {
						vDev = Integer.valueOf(prop.getProperty("default-device"));
					} catch (Exception e) {
					}
					try {
						vX = Integer.valueOf(prop.getProperty("last-x"));
					} catch (Exception e) {
					}
					try {
						vY = Integer.valueOf(prop.getProperty("last-y"));
					} catch (Exception e) {
					}
				} catch (IOException e) {

				}
				try {
					MIDIplayer frame = new MIDIplayer(vX, vY);
					frame.setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
				core = new MIDICore(vDev);
				list.setListData(core.devx);
				list.setSelectedIndex(core.devID);
				txtCurrentdev.setText(list.getSelectedValue());
				txtCurrentdev.setToolTipText("<html>Description:	" + core.devd[list.getSelectedIndex()][0] + "<br>Vendor:		" + core.devd[list.getSelectedIndex()][1] + "<br>Version:	" + core.devd[list.getSelectedIndex()][2] + "</html>");
				Thread cycle = new Thread() {

					public void run() {

						while (true) {
							if (core.isPlaying()) {
								btnPlaypause.setText("Pause");
								if (!isDragging && !isKeySeeking)
									slider.setValue((int) (core.getMidiProg() / 1000));
								if (!isDragging && !isKeySeeking)
									currentTime.setText(microsecondsToTimeString(core.getMidiProg()));
								else
									currentTime.setText(millisecondsToTimeString(slider.getValue()));
							} else {
								btnPlaypause.setText("Play");
								if (!isDragging && !isKeySeeking) {
									if (core.getMidiProg() == core.getLength())
										core.midiNavigate(0);
									slider.setValue((int) (core.midiPauseProgMs / 1000));
								}
								if (!isDragging && !isKeySeeking)
									currentTime.setText(microsecondsToTimeString(core.midiPauseProgMs));
								else
									currentTime.setText(millisecondsToTimeString(slider.getValue()));
							}
							try {
								sleep(25);
							} catch (InterruptedException e) {

							}
						}
					}
				};
				cycle.start();

				if (vLd != null && vLd.exists())
					lastDirectory = vLd;
				tglbtnRepeat.setSelected(vRep);
				if (tglbtnRepeat.isSelected()) {
					core.repeat = cycleType.whole;
				} else {
					core.repeat = cycleType.none;
				}

				String arg = "";
				try {
					arg = args[0];
				} catch (Exception e) {
				}
				File argFs = new File(arg);
				if (arg != "" && (argFs.getName().endsWith(".mid") || argFs.getName().endsWith(".rmi"))
						&& argFs.exists()) {
					midiFile = argFs;
					fName.setText(midiFile.getName());
					core.changeMidi(midiFile, true);
					slider.setMaximum((int) (core.getLength() / 1000));
					totalTime.setText(microsecondsToTimeString(core.getLength()));
					doFileOpened = true;
					lastDirectory = midiFile.getParentFile();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public MIDIplayer(int mX, int mY) {
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				try {
					File propFile = new File("midiplayer.properties");
					if (!propFile.exists()) {
						try {
							propFile.createNewFile();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					Properties prop = new Properties();
					FileInputStream propInputStream = new FileInputStream(propFile);
					prop.load(propInputStream);
					FileOutputStream propOutputStream = new FileOutputStream(propFile);
					prop.setProperty("default-device", String.valueOf(core.devID));
					prop.setProperty("last-y", String.valueOf(getY()));
					prop.setProperty("last-x", String.valueOf(getX()));
					if (lastDirectory != null)
						prop.setProperty("last-directory", lastDirectory.toString());
					else
						prop.setProperty("last-directory", "MIDIPlayer properties");
					prop.setProperty("repeat", String.valueOf(tglbtnRepeat.isSelected()));
					prop.store(propOutputStream, null);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				core.shutdown();
			}
		});
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());// CurrentOS
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		setIconImage(icon);
		setResizable(false);
		setTitle("MIDI Player");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(mX, mY, 600, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JButton btnOpen = new JButton("Open...");
		btnOpen.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				fileOpenHandle();
			}
		});
		btnOpen.setBounds(14, 13, 96, 24);
		contentPane.add(btnOpen);

		fName = new JTextField();
		fName.setFocusable(false);
		fName.setEditable(false);
		fName.setBounds(124, 13, 456, 24);
		contentPane.add(fName);
		fName.setColumns(10);

		slider = new JSlider();
		slider.setMaximum(0);
		slider.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					isKeySeeking = true;
					if (!isDragging && doFileOpened)
						slider.setValue(slider.getValue() - 5000);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					isKeySeeking = true;
					if (!isDragging && doFileOpened)
						slider.setValue(slider.getValue() + 5000);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

				if ((e.getKeyCode() == KeyEvent.VK_LEFT | e.getKeyCode() == KeyEvent.VK_RIGHT) && !isDragging
						&& doFileOpened)
					core.midiNavigate(((long) slider.getValue()) * 1000);
				isKeySeeking = false;
			}
		});
		slider.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {

				isDragging = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {

				if (isDragging) {
					if (doFileOpened)
						core.midiNavigate(((long) slider.getValue()) * 1000);
					isDragging = false;
				}
			}
		});

		slider.setValue(0);
		slider.setBounds(84, 50, 426, 24);
		contentPane.add(slider);

		btnPlaypause = new JButton("Play");
		btnPlaypause.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				core.togglePause();
				if (!doFileOpened) {
					fileOpenHandle();
				}
			}
		});
		btnPlaypause.setBounds(14, 87, 96, 24);
		contentPane.add(btnPlaypause);

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				core.midiStop();
			}
		});
		btnStop.setBounds(124, 87, 96, 24);
		contentPane.add(btnStop);

		btnRew = new JButton("Rew.");
		btnRew.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				core.midiNavigate(core.getMidiProg() - 5000000);
			}
		});
		btnRew.setBounds(234, 87, 96, 24);
		contentPane.add(btnRew);

		btnFf = new JButton("F.F.");
		btnFf.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				core.midiNavigate(core.getMidiProg() + 5000000);
			}
		});
		btnFf.setBounds(344, 87, 96, 24);
		contentPane.add(btnFf);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(14, 161, 566, 191);
		contentPane.add(scrollPane);

		list = new JList<String>();

		scrollPane.setViewportView(list);

		JLabel lblMidiDevice = new JLabel("MIDI Device:");
		lblMidiDevice.setLabelFor(list);
		lblMidiDevice.setBounds(14, 124, 96, 24);
		contentPane.add(lblMidiDevice);

		JButton btnChangeDevice = new JButton("Change Device");
		btnChangeDevice.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				core.changeDev(list.getSelectedIndex());
				txtCurrentdev.setText(list.getSelectedValue());
				txtCurrentdev.setToolTipText("<html>Description:	" + core.devd[list.getSelectedIndex()][0] + "<br>Vendor:		" + core.devd[list.getSelectedIndex()][1] + "<br>Version:	" + core.devd[list.getSelectedIndex()][2] + "</html>");
			}
		});
		btnChangeDevice.setBounds(444, 124, 136, 24);
		contentPane.add(btnChangeDevice);

		currentTime = new JTextField();
		currentTime.setFocusable(false);
		currentTime.setHorizontalAlignment(SwingConstants.TRAILING);
		currentTime.setText("0:00");
		currentTime.setEditable(false);
		currentTime.setBounds(14, 50, 56, 24);
		contentPane.add(currentTime);
		currentTime.setColumns(10);

		totalTime = new JTextField();
		totalTime.setFocusable(false);
		totalTime.setText("0:00");
		totalTime.setHorizontalAlignment(SwingConstants.TRAILING);
		totalTime.setEditable(false);
		totalTime.setColumns(10);
		totalTime.setBounds(524, 50, 56, 24);
		contentPane.add(totalTime);

		tglbtnRepeat = new JToggleButton("Repeat");
		tglbtnRepeat.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (tglbtnRepeat.isSelected()) {
					core.midiLoopStart = 16800;
					core.midiLoopEnd = 125280;
					core.repeat = cycleType.partial;
				} else {
					core.repeat = cycleType.none;
				}
				if (doFileOpened) {
					core.changeCycleMethod();
				}
			}
		});
		tglbtnRepeat.setBounds(454, 87, 126, 24);
		contentPane.add(tglbtnRepeat);

		txtCurrentdev = new JTextField();
		txtCurrentdev.setFocusable(false);
		txtCurrentdev.setEditable(false);
		txtCurrentdev.setBounds(124, 124, 306, 24);
		contentPane.add(txtCurrentdev);
		txtCurrentdev.setColumns(10);

	}

	private void fileOpenHandle() {

		JFileChooser JFileChooser1 = new JFileChooser("");
		JFileChooser1.setCurrentDirectory(lastDirectory);
		JFileChooser1.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {

				return "MIDI Sequences (*.mid, *.rmi)";
			}

			@Override
			public boolean accept(File f) {
				if (f.getName().endsWith(".mid") || f.getName().endsWith(".rmi") || f.isDirectory()) {
					return true;
				} else {
					return false;
				}
			}
		});
		int index = JFileChooser1.showOpenDialog(getContentPane());
		if (index == JFileChooser.APPROVE_OPTION) {
			midiFile = JFileChooser1.getSelectedFile();
			fName.setText(midiFile.getName());
			core.changeMidi(midiFile, true);
			slider.setMaximum((int) (core.getLength() / 1000));
			totalTime.setText(microsecondsToTimeString(core.getLength()));
			doFileOpened = true;
			lastDirectory = JFileChooser1.getCurrentDirectory();
		}
	}

	private static String microsecondsToTimeString(long l) {
		long i = l / 1000000;
		long m = i / 60;
		long s = i % 60;
		String sM = String.valueOf(m);
		String sS = String.valueOf(s);
		if (sS.length() < 2)
			sS = "0" + sS;
		String v = sM + ":" + sS;
		return v;
	}

	private static String millisecondsToTimeString(int l) {
		long i = l / 1000;
		long m = i / 60;
		long s = i % 60;
		String sM = String.valueOf(m);
		String sS = String.valueOf(s);
		if (sS.length() < 2)
			sS = "0" + sS;
		String v = sM + ":" + sS;
		return v;
	}
}
