import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class Spacer extends HBox {
	public Spacer() {
		super();
		setHgrow(this, Priority.ALWAYS);
	}
}
