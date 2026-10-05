# Pipeline de Machine Learning

O dataset não é versionado. Para reproduzir, baixe o **PaySim** pelo [Kaggle](https://www.kaggle.com/datasets/ealaxi/paysim1), respeitando os termos/licença exibidos pela fonte. O dataset é sintético e não representa todo o comportamento de fraude em produção; não contém contexto real de device, país ou merchant, portanto essas features devem ser enriquecidas antes de uso real.

## Dataset canônico de treino

O CSV intermediário deve conter as features de `riskguard_fraud.models.FEATURE_NAMES`, uma coluna `label` binária e, opcionalmente, `timestamp`. A preparação deve documentar missing values, outliers, distribuição das classes, leakage e o mapeamento para as features de negócio.

## Treino e avaliação

```bash
uv sync --extra ml
python ml/train.py --dataset data/features.csv --model logistic --output ml/artifacts/riskguard.joblib
python ml/train.py --dataset data/features.csv --model random-forest --output ml/artifacts/riskguard-rf.joblib
python ml/evaluate.py --dataset data/features.csv --artifact ml/artifacts/riskguard.joblib
```

O split é temporal quando `timestamp` está disponível (80% treino, 20% avaliação). O relatório registra Precision, Recall, F1, ROC-AUC, PR-AUC e matriz de confusão. Artefatos e datasets são ignorados pelo Git; apenas metadados pequenos podem ser versionados.

## Ativação no serviço

`RISK_ENGINE=rules` é o fallback seguro. Para usar um modelo validado, configure `RISK_ENGINE=ml`, `RISK_MODEL_PATH` e, opcionalmente, `RISK_MODEL_FAIL_FAST=true` para impedir inicialização quando o artefato estiver ausente ou incompatível. Toda resposta registra `modelVersion`; em caso de falha, o motor de regras é utilizado por padrão.
