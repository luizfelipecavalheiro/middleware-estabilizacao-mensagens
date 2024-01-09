import java.io.Serializable;
import java.util.Map;

/**
 * Classe que representa uma mensagem serializável.
 * Implementa a interface Serializable.
 * Responsável por armazenar informações sobre a mensagem, como a mensagem em si, o vetor de relógios lógicos e a porta do cliente que enviou a mensagem.
 * Utilizada para enviar mensagens entre os clientes do middleware de causal multicast.
 * Implementa a interface Serializable para permitir a serialização dos objetos.
 * 
 * @author Leonardo Piekala e Luiz Felipe
 *
 */
public class Message implements Serializable{
	private static final long serialVersionUID = 1L;
	
	/** Mensagem */
	public String msg;
	
	/** Vetor de relógios lógicos */
	public Map <Integer,Integer> vectorClock;
	
	/** Porta do cliente que enviou a mensagem */
	public int ClientPort;
	
	/**
	 * Construtor da classe Message.
	 * Cria uma nova instância de Message com os parâmetros fornecidos.
	 * @param ClientPort Porta do cliente que enviou a mensagem.
	 * @param message Mensagem a ser enviada.
	 * @param vectorClock Vetor de relógios lógicos associados à mensagem.
	 */
	public Message(int ClientPort, String message, Map <Integer,Integer> vectorClock) {
		this.ClientPort = ClientPort;
		this.msg = message;
		this.vectorClock = vectorClock;
	}
}


