<script setup>
import { onMounted, ref } from 'vue'
import { getTopCids } from '../services/api'

const emit = defineEmits(['select'])

const topCids = ref([])
const loading = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  loading.value = true
  try {
    topCids.value = await getTopCids()
  } catch {
    errorMessage.value = 'Nao foi possivel carregar os CIDs mais pesquisados.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="top-cids">
    <div class="top-cids__header">
      <span class="section-label">Mais pesquisados</span>
      <strong>Top 5 CIDs</strong>
    </div>

    <p v-if="loading" class="top-cids__status">Carregando...</p>
    <p v-else-if="errorMessage" class="top-cids__status">{{ errorMessage }}</p>
    <p v-else-if="topCids.length === 0" class="top-cids__status">Nenhuma busca registrada ainda.</p>

    <div v-else class="top-cids__grid">
      <button
        v-for="item in topCids"
        :key="item.cidCode"
        type="button"
        class="top-cid-card"
        @click="emit('select', item.cidCode)"
      >
        <span>{{ item.cidCode }}</span>
        <strong>{{ item.total }}</strong>
      </button>
    </div>
  </section>
</template>
