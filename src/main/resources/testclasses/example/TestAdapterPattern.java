

/**
 * Test cases for Adapter Pattern detection
 */
public class TestAdapterPattern {

    // ========== TARGET INTERFACES ==========
    
    /**
     * Target interface that clients expect
     */
    interface MediaPlayer {
        void play(String filename);
    }

    /**
     * Another target interface for a different adapter example
     */
    interface AdvancedMediaPlayer {
        void playVlc(String filename);
        void playMp4(String filename);
    }

    // ========== ADAPTEES (Classes being adapted) ==========
    
    /**
     * Adaptee - existing class with incompatible interface
     */
    static class Mp4Player {
        public void playMp4File(String filename) {
            System.out.println("Playing mp4 file: " + filename);
        }
    }

    /**
     * Another adaptee
     */
    static class VlcPlayer {
        public void playVlcFile(String filename) {
            System.out.println("Playing vlc file: " + filename);
        }
    }

    /**
     * Adaptee with multiple methods
     */
    static class LegacyPrinter {
        public void printDocument(String doc) {
            System.out.println("Printing: " + doc);
        }
        
        public void configure(String config) {
            System.out.println("Configuring: " + config);
        }
    }

    // ========== VALID ADAPTER EXAMPLES ==========

    /**
     * VALID: Classic adapter pattern implementation
     * - Implements target interface (MediaPlayer)
     * - Has adaptee field (Mp4Player)
     * - Constructor takes adaptee and assigns it
     * - Methods delegate to adaptee
     */
    static class ValidMp4Adapter implements MediaPlayer {
        private final Mp4Player mp4Player;

        public ValidMp4Adapter(Mp4Player mp4Player) {
            this.mp4Player = mp4Player;
        }

        @Override
        public void play(String filename) {
            mp4Player.playMp4File(filename);
        }
    }

    /**
     * VALID: Another valid adapter with different adaptee
     */
    static class ValidVlcAdapter implements MediaPlayer {
        private final VlcPlayer vlcPlayer;

        public ValidVlcAdapter(VlcPlayer vlcPlayer) {
            this.vlcPlayer = vlcPlayer;
        }

        @Override
        public void play(String filename) {
            vlcPlayer.playVlcFile(filename);
        }
    }

    /**
     * VALID: Adapter with multiple delegating methods
     */
    interface Printer {
        void print(String doc);
        void setup(String config);
    }

    static class PrinterAdapter implements Printer {
        private final LegacyPrinter legacyPrinter;

        public PrinterAdapter(LegacyPrinter legacyPrinter) {
            this.legacyPrinter = legacyPrinter;
        }

        @Override
        public void print(String doc) {
            legacyPrinter.printDocument(doc);
        }

        @Override
        public void setup(String config) {
            legacyPrinter.configure(config);
        }
    }

    /**
     * VALID: Two-way adapter (adapts in both directions)
     */
    static class TwoWayAdapter implements MediaPlayer, AdvancedMediaPlayer {
        private final Mp4Player mp4Player;

        public TwoWayAdapter(Mp4Player mp4Player) {
            this.mp4Player = mp4Player;
        }

        @Override
        public void play(String filename) {
            mp4Player.playMp4File(filename);
        }

        @Override
        public void playVlc(String filename) {
            // Adapts but delegates to mp4
            mp4Player.playMp4File(filename);
        }

        @Override
        public void playMp4(String filename) {
            mp4Player.playMp4File(filename);
        }
    }

    // ========== INVALID EXAMPLES (Should NOT be detected as adapters) ==========

    /**
     * INVALID: No adaptee field - just implements interface directly
     */
    static class NotAnAdapterNoField implements MediaPlayer {
        @Override
        public void play(String filename) {
            System.out.println("Playing: " + filename);
        }
    }

    /**
     * INVALID: Has field but no delegation in methods
     */
    static class NotAnAdapterNoDelegation implements MediaPlayer {
        private final Mp4Player mp4Player;

        public NotAnAdapterNoDelegation(Mp4Player mp4Player) {
            this.mp4Player = mp4Player;
        }

        @Override
        public void play(String filename) {
            // Doesn't delegate - does its own thing
            System.out.println("Playing without delegation: " + filename);
        }
    }

    /**
     * INVALID: Has field but constructor doesn't initialize it from parameter
     */
    static class NotAnAdapterNoConstructorParam implements MediaPlayer {
        private final Mp4Player mp4Player;

        public NotAnAdapterNoConstructorParam() {
            this.mp4Player = new Mp4Player(); // Creates internally, not passed in
        }

        @Override
        public void play(String filename) {
            mp4Player.playMp4File(filename);
        }
    }

    /**
     * INVALID: Doesn't implement any interface
     */
    static class NotAnAdapterNoInterface {
        private final Mp4Player mp4Player;

        public NotAnAdapterNoInterface(Mp4Player mp4Player) {
            this.mp4Player = mp4Player;
        }

        public void play(String filename) {
            mp4Player.playMp4File(filename);
        }
    }

    /**
     * INVALID: Field is static (shared state, not adapter pattern)
     */
    static class NotAnAdapterStaticField implements MediaPlayer {
        private static Mp4Player mp4Player = new Mp4Player();

        public NotAnAdapterStaticField(Mp4Player player) {
            // Even if constructor has parameter, static field is wrong
        }

        @Override
        public void play(String filename) {
            mp4Player.playMp4File(filename);
        }
    }

    /**
     * INVALID: Adaptee is in same hierarchy (inheritance, not adaptation)
     */
    static class NotAnAdapterSameHierarchy implements MediaPlayer {
        private final MediaPlayer anotherPlayer;

        public NotAnAdapterSameHierarchy(MediaPlayer anotherPlayer) {
            this.anotherPlayer = anotherPlayer;
        }

        @Override
        public void play(String filename) {
            anotherPlayer.play(filename); // This is delegation but same type
        }
    }

    /**
     * INVALID: Has field but it's primitive type
     */
    static class NotAnAdapterPrimitiveField implements MediaPlayer {
        private final int playCount;

        public NotAnAdapterPrimitiveField(int count) {
            this.playCount = count;
        }

        @Override
        public void play(String filename) {
            System.out.println("Play count: " + playCount);
        }
    }

    /**
     * INVALID: Multiple fields but no clear delegation pattern
     */
    static class NotAnAdapterMultipleFields implements MediaPlayer {
        private final Mp4Player mp4Player;
        private final VlcPlayer vlcPlayer;
        private final String name;

        public NotAnAdapterMultipleFields(Mp4Player mp4, VlcPlayer vlc, String name) {
            this.mp4Player = mp4;
            this.vlcPlayer = vlc;
            this.name = name;
        }

        @Override
        public void play(String filename) {
            // Uses neither field for delegation
            System.out.println(name + " playing: " + filename);
        }
    }
}