import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Classe que representa um cliente usando o middleware de causal multicast.
 * Implementa a interface ICausalMulticast.
 * Responsável por enviar mensagens utilizando o CausalMulticastAPI e receber mensagens através do método deliver.
 * @author Leonardo Piekala e Luiz Felipe
 *
 */
public class Client implements ICausalMulticast{
	
	/** API CausalMulticast para o cliente usar */
	private ICausalMulticastAPI causalMulticastAPI;
	
	/**
	 * Método main da classe Client.
	 * Cria uma instância de Client e envia mensagens continuamente.
	 * @param args Argumentos de linha de comando (não utilizado neste caso).
	 */
	public static void main(String[] args) {
		try {
			Client client = new Client();
			while(true) {
				client.sendMessage();				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Construtor da classe Client.
	 * Inicializa a instância de CausalMulticastAPI.
	 */
	public Client() {
		causalMulticastAPI = new CausalMulticast();
	}
	
	/**
	 * Método utilizado para enviar uma mensagem através da API causal multicast.
	 * Lê uma linha de entrada do usuário e chama o método mcsend da API.
	 * @throws IOException Exceção lançada em caso de erro de entrada/saída.
	 */
	public void sendMessage() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String mensagem = reader.readLine();
		causalMulticastAPI.mcsend(mensagem, this);
	}
	
	/**
	 * Método que entrega uma mensagem ao cliente através do callback.
	 * Imprime a mensagem recebida no console.
	 * @param msg Mensagem a ser entregue ao cliente.
	 */
	@Override
	public void deliver(String msg) {
		System.out.println("Mensagem recebida: "+ msg);
	}

}
