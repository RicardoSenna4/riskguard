import {zodResolver} from '@hookform/resolvers/zod';
import {useQuery} from '@tanstack/react-query';
import {Link,useParams} from 'react-router-dom';
import {useForm} from 'react-hook-form';
import {getTransactionDetails,submitReview} from '../lib/api';
import {reviewSchema,ReviewForm} from '../lib/validation';

export function TransactionDetailsPage(){
 const {id=''}=useParams();
 const detail=useQuery({queryKey:['transaction-details',id],queryFn:()=>getTransactionDetails(id)});
 const form=useForm<ReviewForm>({resolver:zodResolver(reviewSchema),defaultValues:{decision:'APPROVE',reason:''}});
 if(detail.isLoading)return <div className="card loading" role="status">Carregando detalhes…</div>;
 if(detail.error||!detail.data)return <div className="alert error" role="alert">Transação não encontrada.</div>;
 const {transaction:t,reasons,timeline,reviews,correlationId}=detail.data;
 async function onSubmit(values:ReviewForm){
  const label=values.decision==='APPROVE'?'aprovar':'bloquear';
  if(!window.confirm(`Confirma ${label} esta transação? Esta ação altera o status final.`))return;
  try{await submitReview(id,values);await detail.refetch();form.reset({decision:values.decision,reason:''});}
  catch(error){form.setError('root',{message:(error as Error).message});}
 }
 return <div className="page">
  <Link className="back" to="/transactions">← Voltar para transações</Link>
  <div className="detail-head"><div><p className="eyebrow">TRANSACTION DETAIL</p><h2>{t.merchant}</h2><span className="muted">{t.externalId} · {t.id}</span></div><span className={`badge ${t.status.toLowerCase()}`}>{t.status}</span></div>
  <div className="detail-grid">
   <section className="card"><h3>Dados da transação</h3><div className="facts"><Fact label="Valor" value={`${t.amount.toLocaleString('pt-BR',{minimumFractionDigits:2})} ${t.currency}`}/><Fact label="Customer" value={t.customerId}/><Fact label="Merchant" value={t.merchant}/><Fact label="Device" value="Não informado pelo evento"/><Fact label="Modelo" value={t.riskModelVersion||'Aguardando análise'}/><Fact label="Risk score" value={t.riskScore==null?'Aguardando':t.riskScore.toFixed(4)}/><Fact label="Correlation ID" value={correlationId||'—'}/></div></section>
   <section className="card"><h3>Timeline</h3><div className="timeline">{timeline.map(h=><div key={h.id}><i/><div><strong>{h.status}</strong><small>{new Date(h.changedAt).toLocaleString('pt-BR')}{h.changedBy?` · ${h.changedBy}`:''}</small></div></div>)}</div></section>
  </div>
  <section className="card"><div className="section-head"><h3>Motivos de alerta</h3><span className="muted">decisão automática</span></div>{reasons.length?<ul className="reason-list">{reasons.map(reason=><li key={reason}>{reason.replace(/_/g,' ')}</li>)}</ul>:<p className="muted">Nenhum motivo registrado.</p>}</section>
  {t.status==='REVIEW'&&<section className="card review-box"><h3>Revisão humana</h3><form onSubmit={form.handleSubmit(onSubmit)}><div className="decision-toggle"><label className={form.watch('decision')==='APPROVE'?'selected':''}><input type="radio" value="APPROVE" {...form.register('decision')}/> Aprovar</label><label className={form.watch('decision')==='BLOCK'?'selected danger-label':''}><input type="radio" value="BLOCK" {...form.register('decision')}/> Bloquear</label></div><textarea aria-label="Motivo da decisão" placeholder="Motivo obrigatório da decisão" {...form.register('reason')}/>{form.formState.errors.reason&&<small className="field-error">{form.formState.errors.reason.message}</small>}{form.formState.errors.root&&<div className="alert error">{form.formState.errors.root.message}</div>}<button className={form.watch('decision')==='BLOCK'?'danger':'primary'} disabled={form.formState.isSubmitting}>{form.formState.isSubmitting?'Salvando…':'Confirmar decisão'}</button></form></section>}
  <section className="card"><h3>Histórico de reviews</h3>{reviews.length?reviews.map(r=><div className="review-item" key={r.id}><b>{r.decision}</b><span>{r.reason}<small>Automática: {r.automaticDecision} · score original {Number(r.originalRiskScore).toFixed(4)}</small></span><small>{new Date(r.reviewedAt).toLocaleString('pt-BR')}</small></div>):<p className="muted">Nenhuma decisão manual registrada.</p>}</section>
 </div>
}
function Fact({label,value}:{label:string;value:string}){return <div><span>{label}</span><strong>{value}</strong></div>}
