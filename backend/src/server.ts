import { app } from './app';
import { env } from './config/env';
import { programarRecordatorios } from './jobs/recordatorios.job';

app.listen(env.port, () => {
  console.log(`PetCare API escuchando en el puerto ${env.port}`);
  if (env.recordatorios.activos) programarRecordatorios();
});
