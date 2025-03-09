package fix.parser.api;

public class MissingTagException extends IllegalArgumentException {

    public MissingTagException(int tag) {
        super(String.format("Tag %s is missing in the message", tag));
    }

}
