import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe que representa o middleware que entrega mensagens em ordem causal.
 * Implementa a interface ICausalMulticast.
 * 
 * @author Leonardo Piekala e Luiz Felipe
 */
public class CausalMulticast implements ICausalMulticastAPI {
	
	/** Receptor de mensagens da  */
	private MessageReceiver messageReceiver;
	
	/** Grupo de clientes, representados pela porta do socket */
	private ArrayList<Integer> grupo;
	
	/** Socket utilizado pelo serviço de descobrimento multicast */
	private MulticastSocket multicastSocket;
	
	/** Socket unicast utilizado para enviar e receber mensagens */
	public DatagramSocket unicastSocket;
	
	/** Endereço multicast para o grupo */
	private static InetAddress multicastAdress;
	
	/** Porta multicast para o grupo */
	private static int MulticastPort = 6789;
	
	/** Endereço localhost utilizado para transmissão unicast */
	private InetAddress LocalHost;
	
	/** Porta do socket unicast do cliente */
	public int ClientPort;
	
	/**
	 * Construtor da classe CausalMulticast.
	 * Inicializa os sockets e realiza a descoberta de outros clientes.
	 */
	public CausalMulticast(){	
		try {
			LocalHost = InetAddress.getLocalHost();
			multicastAdress = InetAddress.getByName("225.0.0.0");
			multicastSocket = new MulticastSocket(MulticastPort);
			messageReceiver = new MessageReceiver(this);
			getClientAddress();
			discoverService();
			messageReceiver.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método que pergunta ao usuário qual porta o cliente quer utilizar.
	 * @throws IOException Exceção lançada em caso de erro de entrada/saída.
	 */
	private void getClientAddress() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Digite a porta do cliente: ");
		String IPePORTA = reader.readLine();
		ClientPort = Integer.parseInt(IPePORTA);
		messageReceiver.vectorClock.put(ClientPort, 0);
	}

	/**
	 * Método que descobre outros clientes até completar 3 clientes no grupo.
	 * @throws IOException Exceção lançada em caso de erro de entrada/saída.
	 * @throws InterruptedException Exceção lançada caso a thread seja interrompida por outra thread.
	 */
	@SuppressWarnings("deprecation")
	private void discoverService() throws IOException, InterruptedException {
		System.out.println("Descobrindo o grupo");
		grupo = new ArrayList<Integer>();
		multicastSocket.joinGroup(multicastAdress);
		byte[] msgBytes = String.format("%d",ClientPort).getBytes(StandardCharsets.UTF_8);
		DatagramPacket ping = new DatagramPacket(msgBytes, msgBytes.length,multicastAdress,MulticastPort);
		multicastSocket.send(ping);
		int recebidos = 0;
		while(recebidos<2) {
			byte[] buf = new byte[20];
			DatagramPacket recv = new DatagramPacket(buf, buf.length);
			multicastSocket.receive(recv);
			String received = new String(buf);
			int portrecv = Integer.parseInt(received.trim());
			if(!grupo.contains(portrecv)  && portrecv != ClientPort) {
				grupo.add(portrecv);
				messageReceiver.vectorClock.put(portrecv, 0);
				recebidos++;
			}
			multicastSocket.send(ping);
			System.out.println("Outros clientes:");
			grupo.forEach(m -> System.out.println(m.toString()));
			Thread.sleep(1000);
		}
	}

	/**
	 * Método utilizado para enviar uma mensagem ao grupo.
	 * 
	 * @param msg Mensagem a ser entregue.
	 * @param client Instância do cliente que será usada para enviar a mensagem a si mesmo por callback.
	 */
	@Override
	public void mcsend(String msg, Client client) {
		try {
			System.out.println("Deseja enviar a mensagem '" + msg + "' a todos? (s/n)");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String enviarParaTodos = reader.readLine();
			client.deliver(msg);
			Map<Integer,Integer> vectorClockCopy = new HashMap<Integer,Integer>();
			messageReceiver.vectorClock.forEach(vectorClockCopy::put);
			messageReceiver.vectorClock.put(ClientPort, messageReceiver.vectorClock.get(ClientPort)+1);
			Message message;
			for (Integer membro: grupo) {
				String enviar = null;
				if(enviarParaTodos.equals("n")) {
					System.out.println("Deseja enviar a mensagem '" + msg + "' para o membro "+ membro+ " ? (s/n)");
					enviar = reader.readLine();
				}
				if(enviarParaTodos.equals("s") || enviar.equals("s") || enviar == null) {
					message = new Message(ClientPort,msg,vectorClockCopy);
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
					objectOutputStream.writeObject(message);
					byte[] data = byteArrayOutputStream.toByteArray();
					DatagramPacket sendPacket = new DatagramPacket(data, data.length,LocalHost, membro);
					unicastSocket.send(sendPacket);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
