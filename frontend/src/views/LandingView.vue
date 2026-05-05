<script setup>
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import BrandLockup from '../components/BrandLockup.vue'

const problemItems = ['Tempo perdido', 'Informação fragmentada', 'Barreiras de idioma']

const steps = [
  {
    number: '01',
    title: 'Informe o CID',
    text: 'Digite o código CID-10 ou uma condição clínica para iniciar a consulta.'
  },
  {
    number: '02',
    title: 'Busca automática no PubMed',
    text: 'O MedScope identifica literatura científica recente e relevante.'
  },
  {
    number: '03',
    title: 'IA gera resumo estruturado',
    text: 'Receba síntese em português, evidência e cautelas para leitura profissional.'
  }
]

const features = [
  'Entrada por CID-10',
  'Resumo em português',
  'IA local para privacidade',
  'Sem custo por requisição',
  'Classificação por evidência'
]

const plans = [
  {
    name: 'Free',
    tone: 'green',
    price: 'Gratuito',
    note: 'Ideal para testes',
    items: ['5 buscas por dia', 'Até 3 CIDs', 'Sem histórico', 'Ideal para testes'],
    button: 'Começar grátis'
  },
  {
    name: 'Pro',
    tone: 'blue',
    price: 'R$ 79',
    period: '/mês',
    badge: 'Mais popular',
    note: 'Mais valor para uso individual recorrente',
    items: ['Buscas ilimitadas', 'Todos os CIDs', 'Histórico completo', 'Exportação em PDF'],
    button: 'Assinar plano Pro',
    featured: true
  },
  {
    name: 'Clínica',
    tone: 'purple',
    price: 'R$ 349',
    period: '/mês',
    note: 'Economia para equipes assistenciais',
    items: ['Multiusuário (5-50 usuários)', 'Dashboard de uso', 'Suporte prioritário'],
    button: 'Falar com equipe'
  },
  {
    name: 'Enterprise',
    tone: 'black',
    price: 'Sob consulta',
    note: 'Para hospitais, redes e operações reguladas',
    items: ['On-premise', 'Integração com prontuários (HL7/FHIR)', 'SLA dedicado'],
    button: 'Solicitar proposta'
  }
]

const audiences = ['Médicos', 'Residentes', 'Clínicas', 'Estudantes']

onMounted(() => {
  const elements = document.querySelectorAll('.reveal')

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('reveal--visible')
          observer.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.14 }
  )

  elements.forEach((element) => observer.observe(element))
})
</script>

<template>
  <div class="landing-page">
    <header class="landing-nav" aria-label="Navegação principal">
      <RouterLink to="/" class="brand-link" aria-label="MedScope">
        <BrandLockup compact />
      </RouterLink>

      <nav class="landing-nav__links" aria-label="Seções">
        <a href="#como-funciona">Como funciona</a>
        <a href="#pricing">Planos</a>
        <a href="#credibilidade">Credibilidade</a>
      </nav>

      <div class="landing-nav__actions">
        <RouterLink class="landing-link" to="/login">Entrar</RouterLink>
        <RouterLink class="landing-button landing-button--small" to="/app">Buscar evidência</RouterLink>
      </div>
    </header>

    <main>
      <section class="landing-hero">
        <img
          class="landing-hero__image"
          src="/images/medscope-hero-clinicians.png"
          alt="Médico usando tablet com dados clínicos em ambiente assistencial moderno"
        />
        <div class="landing-hero__shade" aria-hidden="true"></div>

        <div class="landing-hero__content reveal reveal--visible">
          <span class="landing-eyebrow">Plano gratuito disponível</span>
          <h1>Evidência clínica atualizada, em segundos.</h1>
          <p>
            Busque por CID-10 e receba resumos científicos em português com apoio de IA.
          </p>
          <div class="landing-hero__actions">
            <RouterLink class="landing-button" to="/app">Buscar evidência</RouterLink>
            <a class="landing-button landing-button--secondary" href="#como-funciona">Ver como funciona</a>
          </div>
        </div>
      </section>

      <section class="landing-section problem-section" id="problema">
        <div class="landing-section__inner landing-two-columns">
          <div class="reveal">
            <span class="landing-label">Problema</span>
            <h2>Profissionais de saúde gastam horas buscando evidências em inglês, em múltiplas plataformas.</h2>
          </div>

          <div class="problem-list reveal">
            <div v-for="item in problemItems" :key="item" class="problem-item">
              <span aria-hidden="true"></span>
              <strong>{{ item }}</strong>
            </div>
          </div>
        </div>
      </section>

      <section class="landing-section solution-section">
        <div class="landing-section__inner">
          <div class="section-heading reveal">
            <span class="landing-label">Solução</span>
            <h2>Uma nova forma de acessar evidência clínica</h2>
          </div>

          <div class="flow-visual reveal" aria-label="CID, IA, resumo em português">
            <div>CID</div>
            <span aria-hidden="true"></span>
            <div>IA</div>
            <span aria-hidden="true"></span>
            <div>Resumo em português</div>
          </div>
        </div>
      </section>

      <section class="landing-section how-section" id="como-funciona">
        <div class="landing-section__inner">
          <div class="section-heading reveal">
            <span class="landing-label">Como funciona</span>
            <h2>Do CID ao resumo científico em três passos</h2>
          </div>

          <div class="steps-grid">
            <article v-for="step in steps" :key="step.number" class="step-card reveal">
              <span>{{ step.number }}</span>
              <h3>{{ step.title }}</h3>
              <p>{{ step.text }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="landing-section feature-section">
        <div class="landing-section__inner">
          <div class="section-heading reveal">
            <span class="landing-label">Diferenciais</span>
            <h2>Precisão clínica com fluxo simples para o dia a dia</h2>
          </div>

          <div class="feature-grid">
            <article v-for="feature in features" :key="feature" class="feature-card reveal">
              <span aria-hidden="true"></span>
              <h3>{{ feature }}</h3>
            </article>
          </div>
        </div>
      </section>

      <section class="landing-section demo-section" id="demo">
        <div class="landing-section__inner landing-two-columns">
          <div class="reveal">
            <span class="landing-label">Demo</span>
            <h2>Veja como uma consulta aparece para triagem científica rápida</h2>
            <p class="landing-copy">
              O resumo organiza achados, grau de evidência e nota de cautela, mantendo a decisão no julgamento clínico.
            </p>
          </div>

          <article class="demo-panel reveal">
            <div class="demo-panel__top">
              <strong>CID: F41.1</strong>
              <span>Ansiedade generalizada</span>
            </div>
            <div class="demo-loading">
              <span class="button-spinner" aria-hidden="true"></span>
              <span>Consultando PubMed e estruturando evidência</span>
            </div>
            <div class="demo-result">
              <span>Resumo</span>
              <p>
                Estudos recentes descrevem benefício de psicoterapia estruturada e farmacoterapia selecionada,
                com avaliação individual de resposta e eventos adversos.
              </p>
            </div>
            <div class="demo-result">
              <span>Evidência</span>
              <p>Literatura revisada por pares, com prioridade para revisões e estudos recentes.</p>
            </div>
            <div class="demo-caution">
              Nota de cautela: não substitui julgamento clínico, diretrizes locais ou avaliação individual do paciente.
            </div>
          </article>
        </div>
      </section>

      <section class="landing-section pricing-section" id="pricing">
        <div class="landing-section__inner">
          <div class="section-heading reveal">
            <span class="landing-label">Pricing</span>
            <h2>Comece grátis e escale com previsibilidade</h2>
            <p>Planos para validação individual, uso profissional e implantação B2B.</p>
          </div>

          <div class="pricing-grid">
            <article
              v-for="plan in plans"
              :key="plan.name"
              class="pricing-card reveal"
              :class="[`pricing-card--${plan.tone}`, { 'pricing-card--featured': plan.featured }]"
            >
              <div class="pricing-card__head">
                <div>
                  <span v-if="plan.badge" class="pricing-badge">{{ plan.badge }}</span>
                  <h3>{{ plan.name }}</h3>
                </div>
                <p>{{ plan.note }}</p>
              </div>

              <div class="pricing-card__price">
                <strong>{{ plan.price }}</strong>
                <span v-if="plan.period">{{ plan.period }}</span>
              </div>

              <ul>
                <li v-for="item in plan.items" :key="item">{{ item }}</li>
              </ul>

              <RouterLink class="pricing-button" to="/login">{{ plan.button }}</RouterLink>
            </article>
          </div>
        </div>
      </section>

      <section class="landing-section trust-section" id="credibilidade">
        <div class="landing-section__inner landing-two-columns">
          <div class="reveal">
            <span class="landing-label">Prova e credibilidade</span>
            <h2>Baseado em literatura científica real</h2>
            <p class="landing-copy">
              O MedScope referencia o PubMed para apoiar pesquisa clínica, revisão de hipóteses e atualização científica em português.
            </p>
          </div>

          <div class="trust-panel reveal">
            <strong>PubMed</strong>
            <p>Busca orientada por CID-10, artigos recentes e resumo estruturado por IA.</p>
            <span>Não substitui julgamento clínico.</span>
          </div>
        </div>
      </section>

      <section class="landing-section audience-section">
        <div class="landing-section__inner">
          <div class="section-heading reveal">
            <span class="landing-label">Público-alvo</span>
            <h2>Feito para quem precisa decidir com clareza</h2>
          </div>

          <div class="audience-grid">
            <article v-for="audience in audiences" :key="audience" class="audience-card reveal">
              {{ audience }}
            </article>
          </div>
        </div>
      </section>

      <section class="final-cta">
        <div class="landing-section__inner reveal">
          <h2>Comece gratuitamente e escale conforme sua necessidade</h2>
          <RouterLink class="landing-button" to="/app">Testar MedScope agora</RouterLink>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <div class="landing-section__inner landing-footer__inner">
        <BrandLockup compact />
        <nav aria-label="Links institucionais">
          <a href="#problema">Produto</a>
          <a href="#pricing">Planos</a>
          <a href="#credibilidade">Segurança</a>
          <a href="/login">Login</a>
        </nav>
      </div>
    </footer>
  </div>
</template>
