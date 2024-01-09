import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe que representa o receptor de mensagens unicast de outros middlewares.
 * Estende a classe Thread para permitir a execução em paralelo.
 * Responsável por receber e processar as mensagens unicast enviadas por outros middlewares.
 * Mantém o vetor de relógios lógicos e as mensagens recebidas atrasadas.
 * Verifica a ordem causal das mensagens recebidas e as entrega de acordo com o vetor de relógios.
 * Responsável por imprimir informações sobre os vetores de relógios e as mensagens recebidas.
 * 
 * @author Leonardo Piekala e Luiz Felipe
 *
 */
public class MessageReceiver extends Thread {
	
	/** Middleware associado ao receptor de mensagens */
	private CausalMulticast causalMulticastAPI;
	
	/** Vetor de relógios lógicos associado ao receptor de mensagens */
	public  Map<Integer,Integer> vectorClock;
	
	/** Mensagem recebida (Objeto) */
	private Message receivedMessage;
	
	/** Mensagem recebida (byte array) */
	byte[] receivedMessageByteArray = new byte[1000];
	
	/** Mensagens que serão entregues atrasadas pois há dependência de uma mensagem que ainda não chegou */
	public ArrayList<Message> delayedMessages;
	
	/**
	 * Construtor da classe MessageReceiver.
	 * Cria uma nova instância de MessageReceiver com o middleware associado e inicializa os vetores de relógios e as mensagens atrasadas.
	 * @param causalMulticastAPI Middleware associado ao receptor de mensagens.
	 */
	public MessageReceiver(CausalMulticast causalMulticastAPI) {
		this.causalMulticastAPI = causalMulticastAPI;
		this.vectorClock = new HashMap<Integer,Integer>();
		this.delayedMessages = new ArrayList<Message>();
	}
	
	/** 
	 * Método que recebe mensagens ciclicamente e as processa.
	 * Utiliza sockets unicast para receber as mensagens enviadas por outros middlewares.
	 * Verifica a ordem causal das mensagens recebidas e as entrega de acordo com o vetor de relógios.
	 * Imprime informações sobre os vetores de relógios e as mensagens recebidas.
	 */
	@Override
	public void run() {
		synchronized (this) {
			System.out.println("Pronto para receber mensagens");
			try {
				causalMulticastAPI.unicastSocket = new DatagramSocket(causalMulticastAPI.ClientPort);
				while(true) {
					DatagramPacket receivePacket = new DatagramPacket(receivedMessageByteArray, receivedMessageByteArray.length);
					causalMulticastAPI.unicastSocket.receive(receivePacket);
					byte[] data = receivePacket.getData();
					ByteArrayInputStream byteArrayinputStream = new ByteArrayInputStream(data);
					ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayinputStream);
					receivedMessage = (Message) objectInputStream.readObject();
					printMessageAndReceiverClockVectors();
					Boolean entregar = addMessageToDelayedMessagesIfHasDependency();
					deliverDelayedMessages();			
					if(entregar) {
						deliverMessage();
					}
					deliverDelayedMessages();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/** 
	 * Métodoque verifica se a mensagem recebida possui dependência de mensagens anteriores e a adiciona às mensagens atrasadas, caso necessário.
	 * Verifica se o vetor de relógios está adiantado em relação ao vetor de relógios da mensagem recebida.
	 * @return True se a mensagem não possui dependência de mensagens anteriores e pode ser entregue imediatamente.
	 */
	private Boolean addMessageToDelayedMessagesIfHasDependency() {
		Boolean entregar = true;
		for (Map.Entry<Integer, Integer> entry : vectorClock.entrySet()) {
			int key = entry.getKey();
			int value = entry.getValue();
			if(value < receivedMessage.vectorClock.get(key)) {
				delayedMessages.add(receivedMessage);
				System.out.println("Mensagem '"+receivedMessage.msg+"' esperando para ser entregue");
				entregar = false;
				break;
			}
		}
		return entregar;
	}

	/** 
	 * Método que entrega a mensagem recebida e atualiza o vetor de relógios lógicos.
	 */
	private void deliverMessage() {
		vectorClock.put(receivedMessage.ClientPort, vectorClock.get(receivedMessage.ClientPort)+1);
		System.out.println("Mensagem recebida: " + receivedMessage.msg);
	}

	/** 
	 * Método que imprime no console os vetores de relógios lógicos do receptor do middleware e da mensagem recebida.
	 */
	private void printMessageAndReceiverClockVectors() {
		System.out.println("Vetor atual:");
		for (Map.Entry<Integer, Integer> entry : vectorClock.entrySet()) {
			int key = entry.getKey();
			int value = entry.getValue();
			System.out.println("["+key+"] = ["+value+"]");
		}
		System.out.println("Vetor da mensagem:");
		for (Map.Entry<Integer, Integer> entry : receivedMessage.vectorClock.entrySet()) {
			int key = entry.getKey();
			int value = entry.getValue();
			System.out.println("["+key+"] = ["+value+"]");
		}
	}

	/** 
	 * Método que entrega as mensagens atrasadas, caso não haja dependência de mensagens anteriores.
	 * Verifica se o vetor de relógios do receptor está adiantado em relação ao vetor de relógios de cada mensagem atrasada.
	 */
	private void deliverDelayedMessages() {
		synchronized (this) {
			Boolean entregar = true;
			ArrayList<Message> mensagensASerEntregue = new ArrayList<Message>();
			for(Message mensagemAtrasada : delayedMessages) {
				for (Map.Entry<Integer, Integer> entry : mensagemAtrasada.vectorClock.entrySet()) {
					int key = entry.getKey();
					int value = entry.getValue();
					if(value > vectorClock.get(key)) {
						entregar = false;
						break;
					}
				}
				if(entregar) {
					mensagensASerEntregue.add(mensagemAtrasada);
				}
			}
			for(Message mensagemASerEntregue : mensagensASerEntregue) {
				System.out.println("Mensagem recebida: "+ mensagemASerEntregue.msg);
				//foi entregue, descarta
				delayedMessages.remove(mensagemASerEntregue);
			}
			mensagensASerEntregue.clear();
		}
	}
}
