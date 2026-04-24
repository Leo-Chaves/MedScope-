<script setup>
const props = defineProps({
  article: {
    type: Object,
    required: true
  }
})

function relevanceClass(level) {
  return {
    ALTO: 'relevance-badge--high',
    MEDIO: 'relevance-badge--medium',
    BAIXO: 'relevance-badge--low'
  }[level] || 'relevance-badge--low'
}
</script>

<template>
  <article class="article-card">
    <div class="article-card__top">
      <span class="publication-tag">{{ article.publicationType }}</span>
      <span class="relevance-badge" :class="relevanceClass(article.relevanceLevel)">
        {{ article.relevanceLevel }}
      </span>
    </div>

    <h3>{{ article.title }}</h3>

    <div class="article-data">
      <p><strong>Data:</strong> {{ article.publishedAt || 'Não informada' }}</p>
      <p><strong>Periódico:</strong> {{ article.journal }}</p>
      <p><strong>Tipo de evidência:</strong> {{ article.evidenceType }}</p>
      <p><strong>PubMed ID:</strong> {{ article.pubmedId }}</p>
    </div>

    <section class="article-block">
      <span class="article-block__label">Resumo em português</span>
      <p class="article-summary">{{ article.summaryPt }}</p>
    </section>

    <section class="article-block">
      <span class="article-block__label">Impacto prático</span>
      <p>{{ article.practicalImpact }}</p>
    </section>

    <section class="article-block article-block--warning">
      <span class="article-block__label">Nota de cautela</span>
      <p>{{ article.warningNote }}</p>
    </section>

    <a class="article-link" :href="article.url" target="_blank" rel="noreferrer">
      Abrir artigo no PubMed
    </a>
  </article>
</template>
