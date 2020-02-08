package Entidades;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

public class servidorDNS {

    //private static final Logger LOG = LoggerFactory.getLogger(servidorDNS.class);
    private Thread thread = null;
    private volatile boolean listening = false;
    private boolean running = false;
    private Map<String, String> domainTree = new HashMap();
    private String dirDNSAuxiliar;
    private int versionMF;
    private long TTL;
    private long refreshInterval;
    private retornoMasterFile retorno;
    private int nThreads = 0;
    private List<Thread> threadsSNF = new ArrayList<Thread>();
    private List<DatagramSocket> sockets = new ArrayList<DatagramSocket>();

    private manejadorMastarFile manejadorMF = new manejadorMastarFile();

    public void iniciarServidorDns(final int puertoDeAcceso) {
        if (thread != null) {
            throw new IllegalStateException("El servidor ya esta activo");
        }
        System.out.println("Leer MasterFile:");
        try {

            retorno = manejadorMF.leerMasterFile("m.txt");

            domainTree = retorno.getZonas();
            dirDNSAuxiliar = retorno.getDirDnsAux();
            versionMF = retorno.getVersionMasterFile();
            TTL = retorno.getTTL();
            refreshInterval = retorno.getRefreshInterval();
            System.out.println();
            System.out.println("CARACTERISTICAS DEL SERVIDOR: ");
            System.out.println("******************************");
            System.out.println("Version del master file: " + versionMF);
            System.out.println("TTL a usar: " + TTL);
            System.out.println("Intervalo de actualizacion MasterFile: " + refreshInterval);
            System.out.println("******************************");

        } catch (IOException ex) {
            System.out.println("Error en la lectura del archivo");
        }
        running = true;
        thread = new Thread(new Runnable() {
            public void run() {
                iniciarUDP(puertoDeAcceso);
            }
        });
        thread.setName("servervidor DNS");
        thread.start();
        System.out.println("El servidor DNS se ha iniciado, y usa el puerto " + puertoDeAcceso);
    }

    public void iniciarUDP(int puertoDeAcceso) {
        System.out.println("Inicio del hilo");
        try (DatagramSocket socket = new DatagramSocket(puertoDeAcceso)) {
            listening = true;
            while (running) {
                consultaDNS(socket);
            }
            listening = false;
        } catch (IOException ex) {
            System.out.println("Error al abrir los sockets para UDP" + ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public void consultaDNS(DatagramSocket socket) {
        Name nombreRecurso;
        //System.out.println("Socked abierto");
        try {
            /*Proceso de recepcion de mensaje*/
            //Crear molde DNS
            byte[] in = new byte[512];
            DatagramPacket indp = new DatagramPacket(in, 512);
            indp.setLength(512);
            //Esperar a que un mensaje de Query llegue 
            socket.receive(indp);
            //Creacion de mensaje con el formato DNS
            Message msg = new Message(in);
            //Obtiene el encabezado del mensaje (OPCODE + STATUS + ID)
            Header encabezado = msg.getHeader();
            /*String encabezadoCadena = encabezado.toString();
            System.out.println("Por curiosidad este es el encabezado: " + encabezadoCadena);
            byte[] encabezadoB = encabezado.toWire();
            System.out.println("Cantidad de Bytes del encabezado: -- " + encabezadoB.length);
            System.out.println("*********************************************************");*/
            //Primer registro de la seccion "Pregunta" (-> QNAME + QCLASS + QTYPE)
            Record pregunta = msg.getQuestion();
            //msg.getHeader().
            /*Proceso de Respueta - Armando segun RFC 1035 y 1034 el mensaje de respuesta*/
            //Crear mensaje respuesta con el mismo ID que el de pregunta
            Message respuesta = new Message(encabezado.getID());
            //Especificar que es un mensaje de respuesta
            respuesta.getHeader().setFlag(Flags.AA);
            //Copiar la pregunta del mensaje de pregunta a la seccion correspondiente de la respueta
            respuesta.addRecord(pregunta, Section.QUESTION);
            //Obtener nombre del registro solicitado
            nombreRecurso = pregunta.getName();
            //Ip del cliente DNS
            byte[] ipCliente = indp.getAddress().getAddress();
            //Buscar en el Master File las coincidencias con la consulta
            InetAddress direccion = buscarEnArbol(nombreRecurso.toString(true));
            System.out.println("Nombre del recurso: " + nombreRecurso.toString());
            if (pregunta.getType() == Type.A && direccion != null) {
                //Agrega un recurso a la seccion de respuesta
                System.out.println("*****************************************");
                System.out.println("Pregunta: " + nombreRecurso.toString());
                System.out.println("Direccion asociada: " + domainTree.get(nombreRecurso.toString(true)));
                System.out.println("*****************************************");
                respuesta.addRecord(new ARecord(nombreRecurso, DClass.IN, TTL, direccion), Section.ANSWER);
            } else if (direccion == null) {
                System.out.println("Dominio no encontrado: " + nombreRecurso);
                System.out.println("Redirigiendo DNS a Foreign Name Server");
                /*System.out.println("IP del Cliente DNS "+traducir(indp.getAddress().getAddress()) + "IP del Foreign Name Server "+dirDNSAuxiliar);*/
                
                sockets.add(new DatagramSocket(2000 + nThreads));
                threadsSNF.add(new Thread(new Runnable() {
                    public void run() {
                        if (nThreads - 1 >= 0) {
                            solicitudDNS2(msg, respuesta, indp, socket, nombreRecurso, sockets.get(nThreads - 1));
                        }

                    }
                }));
                
                System.out.println("Número de hilos que están en uso: " + threadsSNF.size());
                if (nThreads - 1 >= 0) {
                    threadsSNF.get(nThreads - 1).setName("servervidor DNS SNF" + (nThreads - 1));
                    threadsSNF.get(nThreads - 1).start();
                }
                nThreads++;
                if (nThreads == 10) {
                    for (DatagramSocket socketIt : sockets) {
                        socketIt.close();
                    }
                    nThreads = 0;
                    threadsSNF.clear();
                    sockets.clear();
                }
                return;
            }
            /*Envio de la respuesta*/
            //Se asigna el formato del mensaje 
            byte[] respuestaB = respuesta.toWire();
            //Construccion del datagrama de envie de respuesta (respuesta, tamano de respuesta, 
            //direccion del cliente, puerto del cliente).
            DatagramPacket outdp = new DatagramPacket(respuestaB, respuestaB.length, indp.getAddress(), indp.getPort());
            //Envio del datagrama usando el socket
            socket.send(outdp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String traducir(byte[] ipCliente) {
        String dirS = "";
        int a = 1;
        for (byte bIter : ipCliente) {
            dirS += Integer.toString(bIter & 0xff);
            dirS += ((a < ipCliente.length) ? "." : "");
            a++;
        }
        return dirS;
    }

    private InetAddress buscarEnArbol(String dominio) throws UnknownHostException {
        //System.out.println("Request recibido para " + dominio);
        //System.out.println("Respuesta "+InetAddress.getByName(domainTree.get(dominio)));
        if (domainTree.get(dominio) != null) {
            System.out.println("Respuesta encontrada en domain Tree: " + domainTree.get(dominio));
            return InetAddress.getByName(domainTree.get(dominio));
        } else {
            return null;
        }
    }

    private String determinarTipo(String resp) {
        StringTokenizer tokenizer = new StringTokenizer(resp);
        ArrayList<String> tokenized = new ArrayList<String>();
        Boolean flag = false;
        int i = 0, ipPos = -1, aux = 0;
        while (tokenizer.hasMoreTokens()) {
            tokenized.add(tokenizer.nextToken());
            //System.out.println("Tonkenizer " + tokenized.get(i));
            if (tokenized.get(i).equals("A")) {
                flag = true;
                ipPos = i + 1;
            }
            aux = i;
            i++;
        }
        if (flag) {
            System.out.println("IP ENCONTRADA " + tokenized.get(ipPos));
            return tokenized.get(ipPos);
        } else {
            System.out.println("Solicitud diferente al tipo A, solicitud del tipo: " + tokenized.get(aux - 1));
            return null;
        }
    }

    private void solicitudDNS2(Message msg, Message respuesta, DatagramPacket indp, DatagramSocket socket, Name nombreRecurso, DatagramSocket socketDNS2) {

        try {
            byte[] rDNS2 = msg.toWire();
            DatagramPacket dpDNS2 = new DatagramPacket(rDNS2, rDNS2.length, InetAddress.getByName(dirDNSAuxiliar), 53);
            byte[] inDNS2 = new byte[512];
            DatagramPacket inDNS2dp = new DatagramPacket(inDNS2, 512);
            inDNS2dp.setLength(512);
            socketDNS2.send(dpDNS2);
            socketDNS2.receive(inDNS2dp);
            System.out.println("IP del DNS que responde: " + traducir(inDNS2dp.getAddress().getAddress()));
            Message msgDNS2 = new Message(inDNS2);
            Record[] n = msgDNS2.getSectionArray(Section.ANSWER);
            //System.out.println("Pruebaaaaaaaaaaa " + n.toString());
            if (n.length > 0) {
                //System.out.println("Pruebaaaaaaaaaaa " + n[0].rdataToString());
                //System.out.println(determinarTipo(n[0].toString()));
                String respDNS2 = determinarTipo(n[0].toString());
                if (respDNS2 != null) {
                    System.out.println("Direccion IP recibida y a enviar: " + respDNS2);
                    System.out.println("************************************************************");
                    respuesta.addRecord(new ARecord(nombreRecurso, DClass.IN, 86000, InetAddress.getByName(respDNS2)), Section.ANSWER);
                    byte[] respuestaA = respuesta.toWire();
                    DatagramPacket outdp = new DatagramPacket(respuestaA, respuestaA.length, indp.getAddress(), indp.getPort());
                    socket.send(outdp);
                }

            }
        } catch (IOException ex) {
            System.out.println("Error al abrir los sockets para DNS Secundario o el socket se ha cerrado-> " + ex.getMessage());
        }
    }
}
