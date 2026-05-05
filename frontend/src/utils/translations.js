const publicationTypeTranslations = {
  'RANDOMIZED CONTROLLED TRIAL': 'Ensaio Clinico Randomizado',
  'CONTROLLED CLINICAL TRIAL': 'Ensaio Clinico Controlado',
  'CLINICAL TRIAL': 'Ensaio Clinico',
  REVIEW: 'Revisao',
  'SYSTEMATIC REVIEW': 'Revisao Sistematica',
  'META-ANALYSIS': 'Meta-analise',
  'JOURNAL ARTICLE': 'Artigo Cientifico',
  'RESEARCH ARTICLE': 'Artigo de Pesquisa',
  'CASE REPORTS': 'Relato de Caso',
  'ARTIGO CIENTIFICO': 'Artigo Cientifico'
}

const relevanceLevelTranslations = {
  ALTO: 'Alta',
  MEDIO: 'Media',
  BAIXO: 'Baixa',
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baixa'
}

export function translatePublicationType(value) {
  return translateDelimitedMetadata(value, publicationTypeTranslations)
}

export function translateRelevanceLevel(value) {
  if (!value) {
    return value
  }
  return relevanceLevelTranslations[value.trim().toUpperCase()] || value
}

function translateDelimitedMetadata(value, dictionary) {
  if (!value) {
    return value
  }

  return value
    .split(',')
    .map((part) => {
      const normalized = part.trim().toUpperCase()
      return dictionary[normalized] || part.trim()
    })
    .filter(Boolean)
    .join(', ')
}
