<script setup>
import { computed } from 'vue'
import ArticleCard from './ArticleCard.vue'

const props = defineProps({
  result: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  hasResults: {
    type: Boolean,
    default: false
  },
  loadingMore: {
    type: Boolean,
    default: false
  }
})

const articleCountLabel = computed(() => {
  const total = props.result?.articles?.length || 0
  if (props.loading && total === 0) {
    return 'Pesquisando artigos'
  }
  return total === 1 ? '1 artigo identificado' : `${total} artigos identificados`
})

function articleKey(article) {
  return `${article.source || 'UNKNOWN'}-${article.sourceId || article.title}`
}
</script>

<template>
  <section class="results-section">
    <div class="results-summary">
      <div class="results-summary__header">
        <div>
          <span class="section-label">Condição mapeada</span>
          <h2>{{ result.condition }}</h2>
        </div>
        <div class="results-pill">{{ loadingMore ? 'Carregando mais' : articleCountLabel }}</div>
      </div>

      <div class="results-meta">
        <article class="meta-card">
          <span class="meta-card__label">CID</span>
          <strong>{{ result.cid }}</strong>
        </article>
        <article class="meta-card meta-card--wide">
          <span class="meta-card__label">Query utilizada</span>
          <strong>{{ result.queryUsed }}</strong>
        </article>
      </div>

      <p class="results-disclaimer">{{ result.disclaimer }}</p>
    </div>

    <section v-if="hasResults" class="article-grid">
      <ArticleCard
        v-for="article in result.articles"
        :key="articleKey(article)"
        :article="article"
      />
    </section>

    <section v-else-if="loading" class="message-card loading-card">
      <div class="loading-card__content">
        <span class="button-spinner loading-card__spinner" aria-hidden="true"></span>
        <div>
          <h2>Buscando evidências</h2>
          <p>
            Estamos consultando as bases científicas e organizando os primeiros artigos para exibição.
          </p>
        </div>
      </div>
    </section>

    <section v-else class="message-card">
      <h2>Nenhum artigo localizado</h2>
      <p>
        A busca foi processada, mas não houve resultados para os parâmetros atuais.
        Revise o CID informado ou refine o contexto clínico.
      </p>
    </section>
  </section>
</template>
