/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package firstAgent;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
	// The catalogue of books for sale (maps the title of a book to its price and state)
	private Hashtable<String, Integer> catalogue;
	// The equivalence table between a state (New, Damaged...) and its numerical value
	private Hashtable state_num;
	// For a state, gives the better state of a book
	private Hashtable<String, String> better_state;
	
	// The GUI by means of which the user can add books in the catalogue
	private BookSellerGui myGui;

	// Put agent initializations here
	protected void setup() {
		// Create the catalogue
		catalogue = new Hashtable();
		
		// create the equivalence table
		state_num = new Hashtable();
		state_num.put("New", 4);
		state_num.put("Good", 3);
		state_num.put("Used", 2);
		state_num.put("Damaged", 1);
		
		better_state = new Hashtable();
		better_state.put("New", "the best");
		better_state.put("Good", "New");
		better_state.put("Used", "Good");
		better_state.put("Damaged", "Used");

		// Create and show the GUI 
		myGui = new BookSellerGui(this);
		myGui.showGui();

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Seller-agent "+getAID().getName()+" terminating.");
	}

	/**
     This is invoked by the GUI when the user adds a new book for sale
	 */
	public void updateCatalogue(final String title, final int price, final String state) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				//new Integer((int)state_num.get(state))
				catalogue.put(title+";"+state, new Integer(price));
				System.out.println(title+" inserted into catalogue of "+getAID().getName()+". State: "+state+". Price = "+price);
			}
		} );
	}

	/**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by Book-seller agents to serve incoming requests 
	   for offer from buyer agents.
	   If the requested book is in the local catalogue the seller agent replies 
	   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	   sent back.
	 */
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String key = msg.getContent();
				System.out.println("recieved the ask for "+msg.getContent());
				String title = key.split(";")[0];
				String state = key.split(";")[1];
				ACLMessage reply = msg.createReply();
				
				//searches the catalogue to see if it contains the book with the required state
				
				if(catalogue.containsKey(key)) {
					//the book is in the catalogue for the given state
					Integer price = (Integer) catalogue.get(key);
					// The requested book is available for sale. Reply with the price
					
					System.out.println("book available");
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue())+";"+state);
				}
				else {
					System.out.println("book not in the catalogue with this state");
					Boolean found = true;
					//maybe the book is in the catalogue with a better state
					String newState;
					while (!catalogue.containsKey(key)){
						newState = (String) better_state.get((String)state);
						if(newState.equals("the best")) {
							found = false;
							break;
						}
						//updates the state with the better one
						key = title+";"+newState;
						state = newState;
					}
					if(found){
						//The book is in the catalogue at a better state than asked
						Integer price = (Integer) catalogue.get(key);
						System.out.println("book available for a better state");
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(String.valueOf(price.intValue())+";"+key.split(";")[1]);
					}
					else {
						//The book is not in the catalogue with at least the required state
						System.out.println("book not available");
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("not-available");
					}
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Book-seller agents to serve incoming 
	   offer acceptances (i.e. purchase orders) from buyer agents.
	   The seller agent removes the purchased book from its catalogue 
	   and replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String key =  msg.getContent();
				String title = msg.getContent().split(";")[0];
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.remove(key);
				if (price != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent "+msg.getSender().getName());
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
}
