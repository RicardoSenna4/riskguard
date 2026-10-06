import {describe,expect,it} from 'vitest';
import {reviewSchema} from './validation';
describe('reviewSchema',()=>{it('accepts a reasoned approval',()=>{expect(reviewSchema.safeParse({decision:'APPROVE',reason:'Documento conferido'}).success).toBe(true)});it('rejects short reasons and unknown decisions',()=>{expect(reviewSchema.safeParse({decision:'BLOCK',reason:'não'}).success).toBe(false);expect(reviewSchema.safeParse({decision:'RELEASE',reason:'valid reason'}).success).toBe(false)})});
