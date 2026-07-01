import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class AGtsp {

    ArrayList<Cidade> cidades = new ArrayList<>();
    private int tamPopulacao;
    private int tamCromossomo = 0;
    private int probMutacao;
    private int qtdeCruzamentos;
    private int numeroGeracoes;
    private ArrayList<ArrayList<Cidade>> populacao = new ArrayList<>();
    private ArrayList<Integer> roletaVirtual = new ArrayList<>();

    public AGtsp(int tamPopulacao, int probMutacao, int qtdeCruzamentos, int numeroGeracoes) {
        this.tamPopulacao = tamPopulacao;
        this.probMutacao = probMutacao;
        this.qtdeCruzamentos = qtdeCruzamentos;
        this.numeroGeracoes = numeroGeracoes;
    }

    public void executar() {
        criarPopulacao();

        for (int i = 0; i < this.numeroGeracoes; i++) {
            novaPopulacao();

            // Exibir a evolução da população a cada geração
            int melhorDaGeracao = obterMelhor();
            double distMelhor = 1.0 / fitness(this.populacao.get(melhorDaGeracao));
            System.out.printf("Geração %d - Melhor distância: %.2f\n", (i + 1), distMelhor);
        }

        int melhor = obterMelhor();
        System.out.println("\nMelhor solução encontrada:");
        mostrarRota(populacao.get(melhor)); // Exibe a melhor rota encontrada e a distância total [cite: 42, 44]
    }

    public void carregarCidades(String arquivo) {
        String linha;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), "UTF-8"))) {
            while ((linha = br.readLine()) != null) {
                String[] dados = linha.split(",");
                String nome = dados[0].trim();
                double x = Double.parseDouble(dados[1]);
                double y = Double.parseDouble(dados[2]);
                Cidade cidade = new Cidade(nome, x, y);
                cidades.add(cidade); // você deve criar a lista cidades: ArrayList<Cidade>
                System.out.println("Carregada: " + cidade);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Cidade> criarCromossomo() {
        ArrayList<Cidade> cromossomo = new ArrayList<>(this.cidades); 
        Collections.shuffle(cromossomo); // 
        return cromossomo;
    }

    private void criarPopulacao() {
        for (int i = 0; i < this.tamPopulacao; i++) {
            this.populacao.add(criarCromossomo());
        }
    }

    private void mostraPopulacao() {
        for (int i = 0; i < this.populacao.size(); i++) {
            double fit = fitness(this.populacao.get(i));
            double distancia = 1.0 / fit;
            System.out.printf("Indivíduo %d | Distância: %.2f | Fitness: %.6f\n", i, distancia, fit);
        }
        System.out.println("-------------------------------");
    }

    private double calcularDistancia(Cidade a, Cidade b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double fitness(ArrayList<Cidade> cromossomo) {
        double distanciaTotal = 0;

        // Somar as distâncias entre cidades consecutivas na rota [cite: 19]
        for (int i = 0; i < cromossomo.size() - 1; i++) {
            distanciaTotal += calcularDistancia(cromossomo.get(i), cromossomo.get(i + 1));
        }

        // Calcular a distancia da ultima cidade para a primeira para fechar o ciclo
        distanciaTotal += calcularDistancia(cromossomo.get(cromossomo.size() - 1), cromossomo.get(0));

        // Quanto menor a distância, melhor, assim retornar o inverso[cite: 20].
        return (1.0 / distanciaTotal);
    }

    //------------------------------
    private void gerarRoleta() {
        this.roletaVirtual.clear();
        for (int i = 0; i < this.populacao.size(); i++) {
            double fit = fitness(this.populacao.get(i));
            // Multiplicamos o fitness (que é pequeno) para criar fatias inteiras proporcionais
            int fatias = (int) (fit * 100000);
            for (int j = 0; j < fatias; j++) {
                this.roletaVirtual.add(i);
            }
        }
    }

    private int roleta() {
        Random rand = new Random();
        int posicao = rand.nextInt(this.roletaVirtual.size());
        return this.roletaVirtual.get(posicao);
    }

    //------------------------------

    public ArrayList<ArrayList<Cidade>> cruzamentoPMX(ArrayList<Cidade> pai1, ArrayList<Cidade> pai2) {
        int tamanho = pai1.size();
        Random rand = new Random();

        // Gerar dois pontos de corte distintos
        int corte1 = rand.nextInt(tamanho);
        int corte2 = rand.nextInt(tamanho);
        if (corte1 > corte2) {
            int temp = corte1;
            corte1 = corte2;
            corte2 = temp;
        }

        // Inicializar filhos com valores nulos
        ArrayList<Cidade> filho1 = new ArrayList<>(Collections.nCopies(tamanho, null));
        ArrayList<Cidade> filho2 = new ArrayList<>(Collections.nCopies(tamanho, null));

        // Copiar segmento entre os cortes
        for (int i = corte1; i <= corte2; i++) {
            filho1.set(i, pai2.get(i));
            filho2.set(i, pai1.get(i));
        }

        // Preencher o restante dos genes respeitando a permutação
        preencherPMX(filho1, pai1, pai2);
        preencherPMX(filho2, pai2, pai1);

        // Retornar os filhos
        ArrayList<ArrayList<Cidade>> filhos = new ArrayList<>();
        filhos.add(filho1);
        filhos.add(filho2);
        return filhos;
    }

    private void preencherPMX(ArrayList<Cidade> filho, ArrayList<Cidade> paiDeOrigem, ArrayList<Cidade> paiDoSegmento) {
        for (int i = 0; i < paiDeOrigem.size(); i++) {
            if (filho.get(i) == null) {
                Cidade geneAInserir = paiDeOrigem.get(i);
                while (filho.contains(geneAInserir)) {
                    int indexNoPaiSegmento = paiDoSegmento.indexOf(geneAInserir);
                    geneAInserir = paiDeOrigem.get(indexNoPaiSegmento);
                }
                filho.set(i, geneAInserir);
            }
        }
    }

    private void mutacao(ArrayList<Cidade> cromossomo) {
        Random rand = new Random();
        // Verifica se a mutação vai ocorrer baseada na probabilidade
        if (rand.nextInt(100) < this.probMutacao) {
            int pos1 = rand.nextInt(cromossomo.size());
            int pos2 = rand.nextInt(cromossomo.size());

            // Troca duas cidades de posição (swap) [cite: 38]
            Collections.swap(cromossomo, pos1, pos2);
        }
    }

    private int obterMelhor() {
        int melhorIndex = 0;
        double melhorFitness = fitness(this.populacao.get(0));

        for (int i = 1; i < this.populacao.size(); i++) {
            double fitAtual = fitness(this.populacao.get(i));
            if (fitAtual > melhorFitness) {
                melhorFitness = fitAtual;
                melhorIndex = i;
            }
        }
        return melhorIndex;
    }

    private int obterPior() {
        int piorIndex = 0;
        double piorFitness = fitness(this.populacao.get(0));

        for (int i = 1; i < this.populacao.size(); i++) {
            double fitAtual = fitness(this.populacao.get(i));
            // O pior indivíduo é o que tem a maior distância, ou seja, o menor fitness
            if (fitAtual < piorFitness) {
                piorFitness = fitAtual;
                piorIndex = i;
            }
        }
        return piorIndex;
    }

    private void novaPopulacao() {
        gerarRoleta();
        ArrayList<ArrayList<Cidade>> novaPop = new ArrayList<>();

        // Elitismo: garante que a melhor rota não seja perdida de uma geração para a outra
        novaPop.add(this.populacao.get(obterMelhor()));

        while (novaPop.size() < this.tamPopulacao) {
            int pai1 = roleta();
            int pai2 = roleta();

            // Cruzamento PMX
            ArrayList<ArrayList<Cidade>> filhos = cruzamentoPMX(this.populacao.get(pai1), this.populacao.get(pai2));

            // Mutação
            mutacao(filhos.get(0));
            mutacao(filhos.get(1));

            novaPop.add(filhos.get(0));
            if (novaPop.size() < this.tamPopulacao) {
                novaPop.add(filhos.get(1));
            }
        }
        this.populacao = novaPop;
    }

    private void operadoresGeneticos() {
        novaPopulacao();
    }

    private void mostrarRota(ArrayList<Cidade> rota) {
        System.out.println("Rota:");
        for (int i = 0; i < rota.size(); i++) {
            System.out.print(rota.get(i).getNome());
            if (i < rota.size() - 1) {
                System.out.print(" -> ");
            }
        }
        // Retorna para a cidade de origem
        System.out.println(" -> " + rota.get(0).getNome());

        // A distância total é o inverso do fitness
        double distanciaTotal = 1.0 / fitness(rota);
        System.out.printf("Distância total: %.2f\n", distanciaTotal);
    }

    public static void main(String[] args) {
        AGtsp ag = new AGtsp(50, 5, 10, 100); // população, mutação, cruzamentos, gerações
        ag.carregarCidades("cidades.csv"); // caminho do arquivo CSV
        ag.executar();
    }

}
