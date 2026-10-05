"""Train RiskGuard models from a local CSV; the dataset is intentionally not committed."""
from __future__ import annotations

import argparse
import json
from datetime import UTC, datetime
from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from riskguard_fraud.models import FEATURE_NAMES


def main()->None:
    parser=argparse.ArgumentParser();parser.add_argument('--dataset',required=True);parser.add_argument('--model',choices=('logistic','random-forest'),default='logistic');parser.add_argument('--output',default='ml/artifacts/riskguard.joblib');args=parser.parse_args()
    frame=pd.read_csv(args.dataset)
    missing=[name for name in [*FEATURE_NAMES,'label'] if name not in frame.columns]
    if missing: raise SystemExit(f'Missing columns: {missing}')
    frame=frame.dropna(subset=[*FEATURE_NAMES,'label']).sort_values('timestamp' if 'timestamp' in frame else FEATURE_NAMES[0])
    split=max(1,int(len(frame)*.8)); train=frame.iloc[:split]
    estimator=LogisticRegression(max_iter=1000,class_weight='balanced') if args.model=='logistic' else RandomForestClassifier(n_estimators=250,random_state=42,class_weight='balanced_subsample',n_jobs=-1)
    model=Pipeline([('scale',StandardScaler()),('classifier',estimator)]) if args.model=='logistic' else estimator
    model.fit(train[list(FEATURE_NAMES)],train['label'].astype(int))
    output=Path(args.output);output.parent.mkdir(parents=True,exist_ok=True)
    joblib.dump({'model':model,'model_version':f'ml-{args.model}-{datetime.now(UTC):%Y%m%d%H%M%S}','features':list(FEATURE_NAMES),'thresholds':{'review':.45,'block':.80}},output)
    output.with_suffix('.json').write_text(json.dumps({'model':args.model,'rows':len(frame),'train_rows':len(train),'features':list(FEATURE_NAMES),'random_state':42},indent=2))
    print(f'wrote {output}')
if __name__=='__main__':main()
