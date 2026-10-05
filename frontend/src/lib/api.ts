export type TransactionStatus='PENDING'|'APPROVED'|'REVIEW'|'BLOCKED'|'REJECTED'|'CANCELLED';
export type Transaction={id:string;customerId:string;externalId:string;amount:number;currency:string;merchant:string;status:TransactionStatus;risk:'UNKNOWN'|'LOW'|'MEDIUM'|'HIGH';riskScore:number|null;riskModelVersion:string|null;version:number;createdAt:string;updatedAt:string};
export type Page<T>={content:T[];page:number;size:number;totalElements:number;totalPages:number;first:boolean;last:boolean};
export type Dashboard={totalTransactions:number;approved:number;review:number;blocked:number;averageRiskScore:number;fraudRate:number;volumeByStatus:{status:string;count:number}[];topReasons:{reason:string;count:number}[];topMerchants:{merchant:string;count:number}[]};
const jsonHeaders={'Content-Type':'application/json'};
export async function api<T>(path:string, options:RequestInit={}){const token=localStorage.getItem('accessToken');const res=await fetch(path,{...options,headers:{...jsonHeaders,...(options.headers||{}),...(token?{Authorization:`Bearer ${token}`}:{})},});if(res.status===401){localStorage.removeItem('accessToken');window.location.href='/login';}if(!res.ok)throw new Error((await res.json().catch(()=>null))?.message||'Não foi possível concluir a operação');return res.status===204?undefined as T:res.json() as Promise<T>}
export const getDashboard=()=>api<Dashboard>('/api/v1/dashboard');
export const getTransactions=(params:URLSearchParams)=>api<Page<Transaction>>(`/api/v1/transactions?${params}`);
export const getTransaction=(id:string)=>api<Transaction>(`/api/v1/transactions/${id}`);
export const getHistory=(id:string)=>api<{id:string;status:TransactionStatus;changedAt:string;changedBy:string|null}[]>(`/api/v1/transactions/${id}/status-history`);
export const getReviews=(id:string)=>api<any[]>(`/api/v1/analyst/transactions/${id}/reviews`);
export const submitReview=(id:string,body:{decision:'APPROVE'|'BLOCK';reason:string})=>api(`/api/v1/analyst/transactions/${id}/reviews`,{method:'POST',body:JSON.stringify(body),headers:{'X-Correlation-Id':crypto.randomUUID()}});
