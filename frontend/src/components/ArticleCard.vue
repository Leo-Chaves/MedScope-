<script setup>
import { translatePublicationType, translateRelevanceLevel } from '../utils/translations'

defineProps({
  article: {
    type: Object,
    required: true
  }
})

function sourceLabel(source) {
  return source === 'SCIELO' ? 'SciELO' : 'PubMed'
}

function relevanceClass(level) {
  const normalized = level?.trim().toUpperCase()
  return {
    ALTO: 'relevance-badge--high',
    MEDIO: 'relevance-badge--medium',
    BAIXO: 'relevance-badge--low',
    HIGH: 'relevance-badge--high',
    MEDIUM: 'relevance-badge--medium',
    LOW: 'relevance-badge--low'
  }[normalized] || 'relevance-badge--low'
}
</script>

<template>
  <article class="article-card">
    <div class="article-card__top">
      <span class="publication-tag">{{ translatePublicationType(article.publicationType) }}</span>
      <span class="relevance-badge" :class="relevanceClass(article.relevanceLevel)">
        {{ translateRelevanceLevel(article.relevanceLevel) }}
      </span>
    </div>

    <h3>{{ article.title }}</h3>

    <div class="article-data">
      <p><strong>Data:</strong> {{ article.publishedAt || 'Nao informada' }}</p>
      <p><strong>Periodico:</strong> {{ article.journal }}</p>
      <p><strong>Tipo de evidencia:</strong> {{ article.evidenceType }}</p>
      <p><strong>Fonte:</strong> {{ sourceLabel(article.source) }}</p>
      <p><strong>{{ sourceLabel(article.source) }} ID:</strong> {{ article.sourceId }}</p>
    </div>

    <section class="article-block">
      <span class="article-block__label">Resumo em portugues</span>
      <p class="article-summary">{{ article.summaryPt }}</p>
    </section>

    <section class="article-block">
      <span class="article-block__label">Impacto pratico</span>
      <p>{{ article.practicalImpact }}</p>
    </section>

    <section class="article-block article-block--warning">
      <span class="article-block__label">Nota de cautela</span>
      <p>{{ article.warningNote }}</p>
    </section>

    <div class="article-card__footer">
      <a class="article-link" :href="article.url" target="_blank" rel="noreferrer">
        <svg viewBox="0 0 24 24" aria-hidden="true" class="article-link__icon">
          <path d="M7 17L17 7" />
          <path d="M9 7h8v8" />
          <path d="M14 17H7V10" />
        </svg>
        <span>Abrir artigo original</span>
      </a>
    </div>
  </article>
</template>
