import java.util.ArrayList;
import java.util.List;

public class SchemaLabel {
    public SchemaLabel() {

    }

    public List<Label> getLabelsDTOShouldHave() {
        List<Label> labels = new ArrayList<>();
        labels.add(Label.DESCRIPTION);
        labels.add(Label.TYPE);
        labels.add(Label.IMPLEMENTATION);
        return labels;
    }

    public List<Label> getLabelsNonDTOShouldHave() {
        List<Label> labels = new ArrayList<>();
        labels.add(Label.DESCRIPTION);
        labels.add(Label.TYPE);
        return labels;
    }

    public enum Label {
        DESCRIPTION, TYPE, IMPLEMENTATION
    }
}
