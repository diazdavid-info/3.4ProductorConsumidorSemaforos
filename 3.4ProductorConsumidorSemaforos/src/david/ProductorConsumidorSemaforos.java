package david;

import java.util.concurrent.Semaphore;

class DatosCompartidos{
	ColaConLimite buffer = new ColaConLimite();
	Semaphore mutex = new Semaphore(1, true);
	Semaphore huecos = new Semaphore(ColaConLimite.TAM_BUFFER, true);
	Semaphore valores = new Semaphore(0, true);
}

class ColaConLimite{
	public static final int TAM_BUFFER = 16;
	protected int[] _valores = new int[TAM_BUFFER];
	volatile protected int _poner = 0;
	volatile protected int _quitar = 0;
	volatile protected int _numElemens = 0;
	
	public void pon(int v){
		if(_numElemens == TAM_BUFFER){
			throw new RuntimeException("No hay hueco");
		}
		
		_valores[_poner] = v;
		_poner = (_poner + 1) % TAM_BUFFER; // Analizar esta linea
		++_numElemens;
	}
	
	public int quitar(){
		if(_numElemens == 0){
			throw new RuntimeException("No hay nada que quitar");
		}
		--_numElemens;
		int indiceDevolver = _quitar;
		_quitar = (_quitar + 1) % TAM_BUFFER;
		return _valores[indiceDevolver];
	}
	
	public boolean vacia() {
		return _numElemens == 0;
	}
	
	public boolean estaLlena() {
		return _numElemens == TAM_BUFFER;
	}
	
	public int numElemens(){
		return _numElemens;
	}
}

class Productor implements Runnable{
	private DatosCompartidos _dc;
	
	public Productor(DatosCompartidos dc) {
		_dc = dc;
	}
	
	public void run(){
		int i = 0;
		while(true){
			++i;
			_dc.huecos.acquireUninterruptibly();
			_dc.mutex.acquireUninterruptibly();
			_dc.buffer.pon(i);
			_dc.mutex.release();
			_dc.valores.release();
		}
	}
}

class Consumidor implements Runnable{
	private DatosCompartidos _dc;
	
	public Consumidor(DatosCompartidos dc){
		_dc = dc;
	}
	
	public void run(){
		int anterior = 0;
		int nuevo;
		
		while(true){
			_dc.valores.acquireUninterruptibly();
			_dc.mutex.acquireUninterruptibly();
			nuevo = _dc.buffer.quitar();
			_dc.mutex.release();
			_dc.huecos.release();
			if((nuevo % 1000) == 0) System.out.println(" "+nuevo);
			if (anterior != (nuevo - 1)) {
				
				System.exit(1);
			}
			anterior = nuevo;
		}
	}
}

public class ProductorConsumidorSemaforos {

	public static void main(String[] args) {
		DatosCompartidos datos = new DatosCompartidos();
		
		Productor p = new Productor(datos);
		Consumidor c = new Consumidor(datos);
		
		new Thread(p, "Productor").start();
		new Thread(c, "Consumidor").start();

	}

}
