import {z} from 'zod';
export const reviewSchema=z.object({decision:z.enum(['APPROVE','BLOCK']),reason:z.string().trim().min(5,'Explique a decisão com pelo menos 5 caracteres.').max(1000)});
export type ReviewForm=z.infer<typeof reviewSchema>;
