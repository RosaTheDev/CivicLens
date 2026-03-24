import { useEffect, useState } from 'react'
import { Badge, Button, Card, Group, Loader, Stack, Text, TextInput } from '@mantine/core'
import uncleSamPoster from '../assets/uncle-sam-go-vote.jpg'

type Election = {
  id: number
  stateCode: string
  officeLevel: string
  title: string
  electionType: string
  electionDate: string
  daysUntil: number
  description: string
}

const EXPLANATIONS = [
  {
    label: 'What is Congress?',
    body: 'Congress is the federal lawmaking branch made up of the House of Representatives and the Senate. Together they draft laws, pass budgets, and oversee federal agencies.',
  },
  {
    label: 'House vs Senate',
    body: 'The House has 435 members and representation is based on state population. The Senate has 100 members total, with exactly 2 senators per state.',
  },
  {
    label: 'Why some states have more representatives',
    body: 'House seats are apportioned by population after each Census. More population means more House seats, while Senate seats remain fixed at two per state.',
  },
  {
    label: 'Primary vs General',
    body: 'Primaries choose party nominees. General elections decide who takes office from those nominees and independent candidates.',
  },
]

export default function ElectionsPage() {
  const [stateCode, setStateCode] = useState('NC')
  const [elections, setElections] = useState<Election[]>([])
  const [loadingElections, setLoadingElections] = useState(true)
  const [now, setNow] = useState(() => new Date())

  async function loadElections(nextState?: string) {
    try {
      setLoadingElections(true)
      const normalizedState = (nextState ?? stateCode).trim().toUpperCase()
      const qs = normalizedState ? `?state=${encodeURIComponent(normalizedState)}` : ''
      const res = await fetch(`/api/elections${qs}`)
      if (!res.ok) {
        throw new Error(`Failed to load elections (${res.status})`)
      }
      const body: Election[] = await res.json()
      setElections(body)
    } catch {
      setElections([])
    } finally {
      setLoadingElections(false)
    }
  }

  useEffect(() => {
    loadElections('NC')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNow(new Date())
    }, 1000)
    return () => window.clearInterval(timer)
  }, [])

  function formatElectionDate(rawDate: string): string {
    const match = rawDate.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (!match) {
      return rawDate
    }
    const year = Number(match[1])
    const month = Number(match[2])
    const day = Number(match[3])
    const date = new Date(year, month - 1, day)
    return new Intl.DateTimeFormat('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }).format(date).replace(',', '')
  }

  function countdownToElection(rawDate: string): string {
    const match = rawDate.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (!match) {
      return 'Date unavailable'
    }
    const year = Number(match[1])
    const month = Number(match[2])
    const day = Number(match[3])
    const electionStart = new Date(year, month - 1, day, 0, 0, 0, 0)
    const diffMs = electionStart.getTime() - now.getTime()
    if (diffMs <= 0) {
      return 'Election day'
    }

    const totalSeconds = Math.floor(diffMs / 1000)
    const days = Math.floor(totalSeconds / 86400)
    const hours = Math.floor((totalSeconds % 86400) / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    const seconds = totalSeconds % 60
    return `${days}d ${hours}h ${minutes}m ${seconds}s`
  }

  return (
    <div className="civic-elections-layout">
      <aside className="civic-elections-aside">
        <Card withBorder className="civic-glass-card civic-uncle-sam-card" padding="xs">
          <img
            src={uncleSamPoster}
            alt="Uncle Sam I Want You poster"
            className="civic-uncle-sam-image"
          />
        </Card>
      </aside>
      <Stack gap="md" className="civic-elections-content">
        <Badge className="civic-pill" variant="light" w="fit-content">
          Civic transparency made simple
        </Badge>
        <div className="civic-hero">
          <Text fw={700} size="xl" className="civic-heading">Elections hub</Text>
          <Text size="sm" c="dimmed">
            Track upcoming election dates, understand primary vs general elections, and find official voting resources.
          </Text>
        </div>

      <Card withBorder className="civic-glass-card">
        <Text fw={600} mb="sm">Election quick guide</Text>
        <Stack gap="xs">
          {EXPLANATIONS.map((entry) => (
            <div key={entry.label}>
              <Text fw={600} size="sm">{entry.label}</Text>
              <Text size="sm" c="dimmed">{entry.body}</Text>
            </div>
          ))}
        </Stack>
      </Card>

      <Card withBorder className="civic-glass-card">
        <Group justify="space-between" align="end">
          <div>
            <Text fw={600}>Upcoming elections</Text>
            <Text size="sm" c="dimmed">Federal elections are always included. Add a state for local context.</Text>
          </div>
          <Group>
            <TextInput
              label="State"
              value={stateCode}
              onChange={(e) => setStateCode(e.currentTarget.value.toUpperCase())}
              placeholder="CA"
              maxLength={2}
              w={90}
            />
            <Button className="civic-action-button" onClick={() => loadElections()}>Refresh</Button>
          </Group>
        </Group>

        <Stack mt="md" gap="sm">
          {loadingElections ? <Loader size="sm" /> : null}
          {!loadingElections && elections.length === 0 ? (
            <Text size="sm" c="dimmed">No upcoming elections found for that filter.</Text>
          ) : null}
          {elections.map((item) => (
            <Card key={item.id} withBorder className="civic-glass-card civic-rep-card" padding="md">
              <Group justify="space-between">
                <div>
                  <Text fw={600}>{item.title}</Text>
                  <Group gap="xs" mt={4}>
                    <Badge className="civic-badge civic-badge--chamber">{item.electionType}</Badge>
                    <Badge className="civic-badge civic-badge--state">{item.stateCode}</Badge>
                    <Text size="sm" c="dimmed">{item.officeLevel}</Text>
                  </Group>
                </div>
                <div className="civic-election-timing">
                  <Text fw={700} className="civic-election-date">{formatElectionDate(item.electionDate)}</Text>
                  <Text size="xs" className="civic-election-countdown-label">
                    Live countdown
                  </Text>
                  <Text fw={800} className="civic-election-countdown">
                    {countdownToElection(item.electionDate)}
                  </Text>
                </div>
              </Group>
              <Text size="sm" c="dimmed" mt="xs">{item.description}</Text>
            </Card>
          ))}
        </Stack>
      </Card>

        <Card withBorder className="civic-glass-card">
          <Text fw={600}>Voting locations & resources</Text>
          <Text size="sm" c="dimmed">
            For accuracy and reliability, use Vote.org&apos;s official polling place lookup.
          </Text>
          <Stack mt="sm" gap="sm">
            <Card withBorder className="civic-glass-card" padding="md">
              <Text fw={600}>National polling place lookup</Text>
              <Text size="sm" c="dimmed" mb={6}>Vote.org polling place locator</Text>
              <Text size="sm" c="dimmed">
                Enter your address on Vote.org to get your assigned polling place and local voting requirements.
              </Text>
              <Group mt="sm">
                <Button
                  className="civic-action-button"
                  component="a"
                  href="https://www.vote.org/polling-place-locator/"
                  target="_blank"
                  rel="noreferrer"
                >
                  Open Vote.org
                </Button>
              </Group>
            </Card>
          </Stack>
        </Card>
      </Stack>
    </div>
  )
}
