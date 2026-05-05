<script setup>
import { computed } from 'vue'
import ArticleCard from './ArticleCard.vue'

const props = defineProps({
  result: {
    type: Object,
    required: true
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

    <section v-else class="message-card">
      <h2>Nenhum artigo localizado</h2>
      <p>
        A busca foi processada, mas não houve resultados para os parâmetros atuais.
        Revise o CID informado ou refine o contexto clínico.
      </p>
    </section>
  </section>
</template>
