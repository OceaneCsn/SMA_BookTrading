package firstAgent;
import jade.core.Agent;

public class HelloAgent extends Agent {
	protected void setup() {
		System.out.println("Hello world, I'm " + this.getLocalName());
	}

}