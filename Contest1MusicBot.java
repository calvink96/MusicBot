import javax.sound.midi.*;
import java.util.*;
import java.io.*;

class Contest1MusicBot{
	class player{
		private boolean noSolo = false;//setting to turn off 'improvisation' over chord progressions
		private BufferedReader scan = new BufferedReader(new InputStreamReader(System.in));
		public Sequencer seq;
		public String scal;
		public String chords;
		public int type;
		public int tempo;
		public int st = -1;
		public ArrayList<getnotes> notelists = new ArrayList<getnotes>(); //for some reason this doesn't work using notelist, had to make an interface and make notelist.notes private
		public player(int typ, String progscale, int temp){
			type = typ;
			tempo = temp;
			if (type==0){
				scal = progscale;
				notelists.add(0, new scale(scal));
			}
			if (type==1){
				chords = progscale;
				String[] cc = chords.split("\\s+");
				for (int i=0;i<cc.length;i++){
					notelists.add(i, new chord(cc[i]));
				}
			}
			try{
				seq = MidiSystem.getSequencer();
				seq.open();
				seq.setTempoInBPM(tempo);
			}
			catch(MidiUnavailableException a){
				System.out.println("No available MIDI device");
				return;
			}
		}
		Sequence genSequence(int[] notes, int len){ //notes is the array of possible notes len = length of sequence in ticks, assume it's bigger than let's say 8
			Random r = new Random();
			Sequence sequ;
			try{
				sequ = new Sequence(Sequence.PPQ, 4);//sixteenth notes
				Track t = sequ.createTrack();
				int c = 0;
				int d = 0;
				if (st==-1){
					st = notes.length/2;//a good starting note; root plus a few octaves, not too low
				}
				if (!noSolo){
					while (c<len-4){
						c = d;
						d+=r.nextInt(3)+1;
						t.add(makeEvent(144, 1, notes[st], 120, c));
						t.add(makeEvent(128, 1, notes[st], 120, d));
						st+= r.nextInt(5)-((st<5)?1:(st>notes.length-5)?3:2);//make it a little bit easier for the music to get out of the low end
						// had a tendency to get stuck playing low notes for a long time, also do the high notes
						if (st<0){ //we don't want any indexing errors 
							st = 0;
						}
						if (st>notes.length-1){
							st = notes.length-1;
						}
					}
					t.add(makeEvent(144, 1, notes[st], 110, d));
					t.add(makeEvent(128, 1, notes[st], 110, len));
				}
				if (type==1){
					t.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 1,50 , 0));
					for (int i=0;i<len/4;i++){
						for (int j=0; j<4;j++){
							t.add(makeEvent(144, 1, notes[20+j], 100, 4*i));
							t.add(makeEvent(128, 1, notes[20+j], 100, 4*i+2));
						}
					}
					t.add(makeEvent(252, len));
					st-=st%4;//if it's a chord progression, make the next chord start on the root
					if (st<12){
						st=12;//prevent getting stuck in the low end
					}
				}
				return sequ;
			}
			catch(InvalidMidiDataException a){
				System.out.println("Caught InvalidMidiDataException");
			}
			return null;
		}
		public int play(){
			try{
				Sequence sequ;
				boolean b = true;
				Sequence[] ns = new Sequence[notelists.size()];
				while (b){
					for (int i=0;i<ns.length;i++){
						ns[i] = genSequence(notelists.get(i).getNotes(), 16);
					}
					for (int i=0;i<ns.length;i++){
						sequ = ns[i];
						seq.setSequence(sequ);
						seq.setTempoInBPM(tempo);
						seq.start();
						while(seq.getTickPosition()<seq.getTickLength()){
							if (scan.ready()){
								b = false;
								break;
							}
						}
						seq.stop();
						if (!b){
							break;
						}
					}
				}
				return 0;
			}
			catch(Exception e){
				e.printStackTrace();
				//something went wrong, return error code
				return 1;
			}
		}
		public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
			MidiEvent ev = null;
			try{
				ShortMessage me = new ShortMessage();
				me.setMessage(comd, chan, one, two);
				ev = new MidiEvent(me, tick);
			}
			catch(InvalidMidiDataException e){
				e.printStackTrace();
			}
			return ev;
		}
		public MidiEvent makeEvent(int comd, int chan, int one, int tick){
			MidiEvent ev = null;
			try{
				ShortMessage me = new ShortMessage();
				me.setMessage(comd, chan, one);
				ev = new MidiEvent(me, tick);
			}
			catch(InvalidMidiDataException e){
				e.printStackTrace();
			}
			return ev;
		}
		public MidiEvent makeEvent(int comd, int tick){
			MidiEvent ev = null;
			try{
				ShortMessage me = new ShortMessage();
				me.setMessage(comd);
				ev = new MidiEvent(me, tick);
			}
			catch(InvalidMidiDataException e){
				e.printStackTrace();
			}
			return ev;
		}
		public void setnoSolo(boolean b){
			noSolo = b;
		}
	}
	interface getnotes{
		public int[] getNotes();
	}
	class notelist implements getnotes{
		public int rootnote;
		public String root;
		public String modifiers;
		public int[] notes;
		public notelist(String string){
			if (string.contains("#")||string.contains("b")){
				root = string.substring(0,2);
			}
			else{
				root = string.substring(0,1);
			}
			rootnote = getRootNoteFromRoot(root);
			modifiers = "";
		}
		public int getRootNoteFromRoot(String root){
			switch(root){
				case "C": 	return 0;
				case "C#":
				case "Db":	return 1;
				case "D": 	return 2;
				case "D#":
				case "Eb":	return 3;
				case "E": 	return 4;
				case "F":	return 5;
				case "F#":
				case "Gb":	return 6;
				case "G": 	return 7;
				case "G#":
				case "Ab":	return 8;
				case "A": 	return 9;
				case "A#":
				case "Bb":	return 10;
				case "B": 	return 11;
			}
			return -1;
		}
		public int[] getNotes(){
			return notes;
		}
	}
	class scale extends notelist{
		private int[] notes = new int[70];
		public scale(String scal){
			super(scal);
			if (scal.contains("m"))
				modifiers = "m";
			else
				modifiers = "";
			for (int i=0;i<10;i+=1){ //set scale notes maj:WWHWWWH, min:WHWWHWW
				int as = 7*i;//notes shift
				int shift = rootnote+12*i;//sets octave
				notes[as] = 	shift;
				notes[as+1] = 	shift+2;
				notes[as+2] = 	modifiers=="m"?shift+3:shift+4;
				notes[as+3] =	shift+5;
				notes[as+4] =	shift+7;
				notes[as+5] =	modifiers=="m"?shift+8:shift+9;
				notes[as+6] =	modifiers=="m"?shift+10:shift+11;
			}
			for (int i=0;i<70;i+=1){
				if (notes[i]>127){
					notes[i] = rootnote+48; //nice reasonable note
				}
			}
		}
		public int[] getNotes(){
			return notes;
		}
	}
	class chord extends notelist{
		private int[] notes = new int[40];
		public chord(String chor){
			super(chor);
			String modifiers;
			if (chor.contains("m7"))
				modifiers = "m7";
			else if (chor.contains("M7"))
				modifiers = "M7";
			else if (chor.contains("7"))
				modifiers = "7";
			else if (chor.contains("m"))
				modifiers = "m";
			else
				modifiers = "";
			for (int i=0;i<10;i+=1){
				int as = 4*i;
				int shift = 12*i+rootnote;
				notes[as] = shift;
				notes[as+1] = modifiers.contains("m")?shift+3:shift+4;
				notes[as+2] = shift+7;
				notes[as+3] = (!modifiers.contains("7"))?shift:(modifiers.contains("M")?shift+11:shift+10);
			}
			for (int i=0;i<40;i+=1){
				if (notes[i]>127){
					notes[i] = rootnote+36; //nice reasonable note
				}
			}
		}
		public int[] getNotes(){
			return notes;
		}
	}
	private Scanner console;
	public int SCALE = 0;
	public int CHORD = 1;
	public static void main(String[] args){
		Contest1MusicBot bot = new Contest1MusicBot();
		bot.mainLoop();
	}
	public void processinput(String input){
		if (input.toLowerCase().contains("roxanne")){
			player p = new player(CHORD, "Gm Dm D#M7 Dm Cm F G G", 131);
			p.setnoSolo(true);
			if (p.play()==0){
				String s = console.nextLine();//for some reason this makes "Please enter a command" not print twice
			}
			return;
		}
		if (input.contains("scale")){
			System.out.print("Scale: ");
			String scal = console.nextLine();
			System.out.print("Tempo: ");
			int tempo = Integer.parseInt(console.nextLine());
			player p = new player(SCALE, scal, tempo);
			if (p.play()==0){
				String s = console.nextLine();//for some reason this makes "Please enter a command" not print twice
			}
			return;
		}
		if (input.contains("chords")){
			System.out.print("Chord Progression: ");
			String chords = console.nextLine();
			System.out.print("Tempo: ");
			int tempo = Integer.parseInt(console.nextLine());
			player p = new player(CHORD, chords, tempo);
			if (p.play()==0){
				String s = console.nextLine();
			}
			
			return;
		}
		if (input.contains("help")){
			System.out.println("Help -- MusicBot --");
			System.out.println("Notes: C, C#, Db, D, D#, Eb, E, F, F#, Gb, G, G#, Ab, A, A#, Bb, and B");
			System.out.println("Tempos must be between 1 and 200");
			System.out.println("Help -- Commands --");
			System.out.println("scale\t\t\tallows you to type in a scale to play music based off of");
			System.out.println("chords\t\t\tallows you to type a chord progression to play");
			System.out.println("Help -- Scale --");
			System.out.println("Upon typing scale, MusicBot will ask for a scale and a tempo");
			System.out.println("Valid scales are the same as the note names. Note names followed by an \"m\" will be made minor");
			System.out.println("Help -- Chords --");
			System.out.println("Upon typing chord, Musicbot will ask for a chord progression and a tempo");
			System.out.println("Chords progressions can be any sequence of chord names seperated by spaces");
			System.out.println("Chords names are note names followed by nothing, m, 7, m7, or M7");
		}
		return;
	}
	public void mainLoop(){
		System.out.println("Welcome to MusicBot");
		console = new Scanner(System.in);
		System.out.println("Type \"quit\" to exit program");
		System.out.print("Please enter a command: ");
		String input = console.nextLine();
		while(!input.toLowerCase().contains("quit")){
			processinput(input);
			System.out.print("Please enter a command: ");
			input = console.nextLine();
		}
	}

}
